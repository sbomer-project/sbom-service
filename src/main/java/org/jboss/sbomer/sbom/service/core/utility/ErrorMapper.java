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
 * This mapper provides a centralized place to define how different error sources
 * are translated into service-owned canonical error codes, ensuring consistency
 * across REST responses, logs, persistence, and Kafka notifications.
 */
@Slf4j
public class ErrorMapper {

    private ErrorMapper() {
        // Utility class
    }

    /**
     * Maps a Java exception to a canonical error result code.
     * 
     * @param exception The exception to map
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
     * This provides backward compatibility during migration while establishing
     * canonical codes as the primary error identity.
     * 
     * @param generationResult The legacy generation result code
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
     * This provides backward compatibility during migration while establishing
     * canonical codes as the primary error identity.
     * 
     * @param enhancementResult The legacy enhancement result code
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
