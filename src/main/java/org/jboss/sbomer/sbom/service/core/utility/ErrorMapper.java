package org.jboss.sbomer.sbom.service.core.utility;

import java.util.Optional;

import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationResult;
import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;
import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;
import org.jboss.sbomer.sbom.service.core.domain.exception.ValidationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility for mapping exceptions and external worker failures to canonical error codes.
 *
 * This mapper is the translation boundary between external or legacy failure signals and
 * the service-owned {@link ErrorResult} taxonomy. It centralizes classification so retry
 * logic, persistence, REST responses, logs, and Kafka notifications all use the same
 * canonical error identity.
 *
 * Mapping rationale:
 * <ul>
 *   <li>Validation and state exceptions are mapped to non-retryable validation results.</li>
 *   <li>Legacy generation and enhancement worker result codes are normalized into stable canonical codes.</li>
 *   <li>Success values map to {@link Optional#empty()} because they do not represent an error.</li>
 *   <li>Unknown or generic worker failures fall back to execution-failed canonical results.</li>
 *   <li>Unexpected Java exceptions fall back to {@link ErrorResult#UNEXPECTED_ERROR}.</li>
 * </ul>
 *
 * Representative mappings:
 * <ul>
 *   <li>{@code ValidationException -> INVALID_REQUEST}</li>
 *   <li>{@code EntityNotFoundException -> ENTITY_NOT_FOUND}</li>
 *   <li>{@code ERR_OOM -> EXTERNAL_RESOURCE_EXHAUSTED}</li>
 *   <li>{@code ERR_SYSTEM -> EXTERNAL_SYSTEM_ERROR}</li>
 *   <li>{@code ERR_INDEX_INVALID -> INVALID_TARGET}</li>
 *   <li>{@code ERR_ENHANCEMENT -> ENHANCER_EXECUTION_FAILED}</li>
 * </ul>
 */
@Slf4j
public class ErrorMapper {

    private ErrorMapper() {
        // Utility class
    }

    /**
     * Maps a Java exception to a canonical error result code.
     *
     * The mapping favors stable service semantics over exception-type leakage. For example,
     * validation-related exceptions become {@link ErrorResult#INVALID_REQUEST}, missing
     * entities become {@link ErrorResult#ENTITY_NOT_FOUND}, and uncategorized exceptions
     * become {@link ErrorResult#UNEXPECTED_ERROR}.
     *
     * @param exception the exception to map
     * @return Optional containing the canonical error result code, or empty if no mapping exists
     */
    public static Optional<ErrorResult> fromException(Exception exception) {
        if (exception instanceof ValidationException) {
            return Optional.of(ErrorResult.INVALID_REQUEST);
        }
        
        if (exception instanceof EntityNotFoundException) {
            return Optional.of(ErrorResult.ENTITY_NOT_FOUND);
        }
        
        if (exception instanceof InvalidRetryStateException) {
            return Optional.of(ErrorResult.INVALID_STATE_TRANSITION);
        }
        
        if (exception instanceof IllegalStateException) {
            // Context-dependent: could be orchestration or internal error
            // Default to internal processing error
            log.warn("Mapping IllegalStateException to INTERNAL_PROCESSING_ERROR. Consider using more specific exception types.");
            return Optional.of(ErrorResult.INTERNAL_PROCESSING_ERROR);
        }
        
        if (exception instanceof IllegalArgumentException) {
            return Optional.of(ErrorResult.INVALID_REQUEST);
        }
        
        // Default catch-all for unexpected exceptions
        log.error("Mapping unexpected exception type {} to UNEXPECTED_ERROR", exception.getClass().getSimpleName());
        return Optional.of(ErrorResult.UNEXPECTED_ERROR);
    }

    /**
     * Maps a legacy GenerationResult to a canonical error result code.
     *
     * This preserves backward compatibility with generator worker contracts while ensuring
     * that retry and persistence decisions use the stable {@link ErrorResult} model.
     * Examples include {@code ERR_OOM -> EXTERNAL_RESOURCE_EXHAUSTED},
     * {@code ERR_SYSTEM -> EXTERNAL_SYSTEM_ERROR}, and
     * {@code ERR_INDEX_INVALID -> INVALID_TARGET}.
     *
     * @param generationResult the legacy generation result code
     * @return Optional containing the canonical error result code, or empty for SUCCESS
     */
    public static Optional<ErrorResult> fromGenerationResult(GenerationResult generationResult) {
        switch (generationResult) {
            case SUCCESS:
                // Not an error
                return Optional.empty();
                
            case ERR_CONFIG_INVALID:
            case ERR_CONFIG_MISSING:
                return Optional.of(ErrorResult.EXTERNAL_BAD_CONFIGURATION);
                
            case ERR_OOM:
                return Optional.of(ErrorResult.EXTERNAL_RESOURCE_EXHAUSTED);
                
            case ERR_SYSTEM:
                return Optional.of(ErrorResult.EXTERNAL_SYSTEM_ERROR);
                
            case ERR_INDEX_INVALID:
                return Optional.of(ErrorResult.INVALID_TARGET);
                
            case ERR_GENERATION:
            case ERR_POST:
            case ERR_MULTI:
                return Optional.of(ErrorResult.GENERATOR_EXECUTION_FAILED);
                
            case ERR_GENERAL:
            default:
                return Optional.of(ErrorResult.GENERATOR_EXECUTION_FAILED);
        }
    }

    /**
     * Maps a legacy EnhancementResult to a canonical error result code.
     *
     * This preserves backward compatibility with enhancer worker contracts while converting
     * retry decisions to canonical service-owned error identities. For example,
     * {@code ERR_CONFIG_INVALID} maps to {@link ErrorResult#EXTERNAL_BAD_CONFIGURATION}
     * and {@code ERR_ENHANCEMENT} maps to {@link ErrorResult#ENHANCER_EXECUTION_FAILED}.
     *
     * @param enhancementResult the legacy enhancement result code
     * @return Optional containing the canonical error result code, or empty for SUCCESS
     */
    public static Optional<ErrorResult> fromEnhancementResult(EnhancementResult enhancementResult) {
        switch (enhancementResult) {
            case SUCCESS:
                // Not an error
                return Optional.empty();
                
            case ERR_CONFIG_INVALID:
                return Optional.of(ErrorResult.EXTERNAL_BAD_CONFIGURATION);
                
            case ERR_ENHANCEMENT:
                return Optional.of(ErrorResult.ENHANCER_EXECUTION_FAILED);
                
            case ERR_GENERAL:
            default:
                return Optional.of(ErrorResult.ENHANCER_EXECUTION_FAILED);
        }
    }

    /**
     * Maps a numeric generation result code directly to a canonical error result.
     *
     * This is a convenience method for Kafka adapters that receive numeric result codes
     * from external workers. It combines code parsing and error mapping in one step.
     *
     * <p><b>Important:</b> Returns {@code null} for success (resultCode == 0), which indicates
     * no error occurred. Any non-null ErrorResult indicates a failure.</p>
     *
     * @param resultCode The numeric result code from the external worker (0, 1, 5, 7, etc.)
     * @return The canonical error result, or <b>null if the code represents success</b>
     */
    public static ErrorResult fromGenerationResultCode(int resultCode) {
        return GenerationResult.fromCode(resultCode)
                .flatMap(ErrorMapper::fromGenerationResult)
                .orElse(ErrorResult.GENERATOR_EXECUTION_FAILED);
    }

    /**
     * Maps a numeric enhancement result code directly to a canonical error result.
     *
     * This is a convenience method for Kafka adapters that receive numeric result codes
     * from external workers. It combines code parsing and error mapping in one step.
     *
     * <p><b>Important:</b> Returns {@code null} for success (resultCode == 0), which indicates
     * no error occurred. Any non-null ErrorResult indicates a failure.</p>
     *
     * @param resultCode The numeric result code from the external worker (0, 1, 2, 5, etc.)
     * @return The canonical error result, or <b>null if the code represents success</b>
     */
    public static ErrorResult fromEnhancementResultCode(int resultCode) {
        return EnhancementResult.fromCode(resultCode)
                .flatMap(ErrorMapper::fromEnhancementResult)
                .orElse(ErrorResult.ENHANCER_EXECUTION_FAILED);
    }

    /**
     * Builds a human-readable reason message for a generation failure.
     * 
     * @param generationResult The legacy generation result
     * @param targetIdentifier The target being processed (e.g., image reference)
     * @param upstreamReason Optional upstream reason from external worker
     * @return A descriptive reason message
     */
    public static String buildGenerationFailureReason(
            GenerationResult generationResult,
            String targetIdentifier,
            String upstreamReason) {
        
        String baseMessage = switch (generationResult) {
            case ERR_CONFIG_INVALID -> "Generator received invalid configuration";
            case ERR_CONFIG_MISSING -> "Generator configuration is missing";
            case ERR_OOM -> "Generator ran out of memory";
            case ERR_SYSTEM -> "Generator encountered a system error";
            case ERR_INDEX_INVALID -> "Generator failed due to invalid index";
            case ERR_POST -> "Generator post-processing failed";
            case ERR_MULTI -> "Generator encountered multiple errors";
            case ERR_GENERATION -> "Generator execution failed";
            default -> "Generator failed";
        };
        
        StringBuilder reason = new StringBuilder(baseMessage);
        
        if (targetIdentifier != null && !targetIdentifier.isEmpty()) {
            reason.append(" for target ").append(targetIdentifier);
        }
        
        if (upstreamReason != null && !upstreamReason.isEmpty() && !upstreamReason.equals("TaskRunFailed")) {
            reason.append(": ").append(upstreamReason);
        }
        
        return reason.toString();
    }

    /**
     * Builds a human-readable reason message for an enhancement failure.
     * 
     * @param enhancementResult The legacy enhancement result
     * @param enhancerName The name of the enhancer that failed
     * @param upstreamReason Optional upstream reason from external worker
     * @return A descriptive reason message
     */
    public static String buildEnhancementFailureReason(
            EnhancementResult enhancementResult,
            String enhancerName,
            String upstreamReason) {
        
        String baseMessage = switch (enhancementResult) {
            case ERR_CONFIG_INVALID -> "Enhancer received invalid configuration";
            case ERR_ENHANCEMENT -> "Enhancer execution failed";
            default -> "Enhancer failed";
        };
        
        StringBuilder reason = new StringBuilder(baseMessage);
        
        if (enhancerName != null && !enhancerName.isEmpty()) {
            reason.append(" (").append(enhancerName).append(")");
        }
        
        if (upstreamReason != null && !upstreamReason.isEmpty() && !upstreamReason.equals("TaskRunFailed")) {
            reason.append(": ").append(upstreamReason);
        }
        
        return reason.toString();
    }
}
