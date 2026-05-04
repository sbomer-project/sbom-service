package org.jboss.sbomer.sbom.service.core.utility;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.jboss.sbomer.events.common.FailureSpec;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility for building FailureSpec objects with canonical error codes.
 * 
 * This utility maps various error sources (exceptions, external worker failures)
 * into standardized FailureSpec objects using the result+reason+status pattern.
 * External/upstream reasons are preserved in the details map for diagnostics.
 */
@Slf4j
public class FailureUtility {

    private FailureUtility() {
        // Utility class
    }

    /**
     * Builds a FailureSpec from a Java Exception using canonical error codes.
     *
     * @param exception The exception that was caught
     * @return A populated FailureSpec with canonical error code
     */
    public static FailureSpec buildFailureSpecFromException(Exception exception) {
        ErrorResult errorResult = ErrorMapper.fromException(exception);
        
        FailureSpec failure = new FailureSpec();
        
        // Use canonical error code as primary errorCode
        failure.setErrorCode(errorResult.name());
        
        // Build human-readable reason
        String reason = exception.getMessage();
        if (reason == null || reason.isEmpty()) {
            reason = "An error occurred: " + exception.getClass().getSimpleName();
        }
        failure.setReason(reason);

        // Capture diagnostic details
        Map<String, String> details = new HashMap<>();
        
        // Add exception class as upstream diagnostic
        details.put("exceptionClass", exception.getClass().getName());
        
        // Add error metadata
        details.put("category", errorResult.getCategory().name());
        details.put("retryable", String.valueOf(errorResult.isRetryable()));
        details.put("ownership", errorResult.getOwnership().name());
        details.put("severity", errorResult.getSeverity().name());
        
        // Capture stack trace for internal diagnostics
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();
        
        // Limit stack trace size to avoid excessive payload
        if (stackTrace.length() > 5000) {
            stackTrace = stackTrace.substring(0, 5000) + "\n... (truncated)";
        }
        details.put("stackTrace", stackTrace);
        
        failure.setDetails(details);

        return failure;
    }

    /**
     * Builds a FailureSpec from a generation failure using canonical error codes.
     *
     * @param generationResult The legacy generation result code
     * @param targetIdentifier The target being processed (e.g., image reference)
     * @param upstreamReason The raw reason from the external worker
     * @param generationId The generation ID
     * @param runId The run ID
     * @return A populated FailureSpec with canonical error code
     */
    public static FailureSpec buildFailureSpecFromGenerationFailure(
            GenerationResult generationResult,
            String targetIdentifier,
            String upstreamReason,
            String generationId,
            String runId) {
        
        ErrorResult errorResult = ErrorMapper.fromGenerationResult(generationResult);
        
        FailureSpec failure = new FailureSpec();
        
        // Use canonical error code as primary errorCode
        failure.setErrorCode(errorResult.name());
        
        // Build human-readable reason with context
        String reason = ErrorMapper.buildGenerationFailureReason(
            generationResult, 
            targetIdentifier, 
            upstreamReason
        );
        failure.setReason(reason);

        // Capture diagnostic details
        Map<String, String> details = new HashMap<>();
        
        // Preserve upstream/legacy information
        details.put("upstreamReason", upstreamReason != null ? upstreamReason : "");
        details.put("legacyResultCode", String.valueOf(generationResult.getCode()));
        details.put("legacyResultName", generationResult.name());
        
        // Add error metadata
        details.put("category", errorResult.getCategory().name());
        details.put("retryable", String.valueOf(errorResult.isRetryable()));
        details.put("ownership", errorResult.getOwnership().name());
        details.put("severity", errorResult.getSeverity().name());
        
        // Add context IDs
        if (generationId != null) {
            details.put("generationId", generationId);
        }
        if (runId != null) {
            details.put("runId", runId);
        }
        if (targetIdentifier != null) {
            details.put("targetIdentifier", targetIdentifier);
        }
        
        failure.setDetails(details);

        log.debug("Built FailureSpec for generation failure: result={} reason={} upstreamReason={}", 
                  errorResult, reason, upstreamReason);

        return failure;
    }

    /**
     * Builds a FailureSpec from an enhancement failure using canonical error codes.
     *
     * @param enhancementResult The legacy enhancement result code
     * @param enhancerName The name of the enhancer that failed
     * @param upstreamReason The raw reason from the external worker
     * @param enhancementId The enhancement ID
     * @param runId The run ID
     * @return A populated FailureSpec with canonical error code
     */
    public static FailureSpec buildFailureSpecFromEnhancementFailure(
            EnhancementResult enhancementResult,
            String enhancerName,
            String upstreamReason,
            String enhancementId,
            String runId) {
        
        ErrorResult errorResult = ErrorMapper.fromEnhancementResult(enhancementResult);
        
        FailureSpec failure = new FailureSpec();
        
        // Use canonical error code as primary errorCode
        failure.setErrorCode(errorResult.name());
        
        // Build human-readable reason with context
        String reason = ErrorMapper.buildEnhancementFailureReason(
            enhancementResult, 
            enhancerName, 
            upstreamReason
        );
        failure.setReason(reason);

        // Capture diagnostic details
        Map<String, String> details = new HashMap<>();
        
        // Preserve upstream/legacy information
        details.put("upstreamReason", upstreamReason != null ? upstreamReason : "");
        details.put("legacyResultCode", String.valueOf(enhancementResult.getCode()));
        details.put("legacyResultName", enhancementResult.name());
        
        // Add error metadata
        details.put("category", errorResult.getCategory().name());
        details.put("retryable", String.valueOf(errorResult.isRetryable()));
        details.put("ownership", errorResult.getOwnership().name());
        details.put("severity", errorResult.getSeverity().name());
        
        // Add context IDs
        if (enhancementId != null) {
            details.put("enhancementId", enhancementId);
        }
        if (runId != null) {
            details.put("runId", runId);
        }
        if (enhancerName != null) {
            details.put("enhancerName", enhancerName);
        }
        
        failure.setDetails(details);

        log.debug("Built FailureSpec for enhancement failure: result={} reason={} upstreamReason={}", 
                  errorResult, reason, upstreamReason);

        return failure;
    }
}