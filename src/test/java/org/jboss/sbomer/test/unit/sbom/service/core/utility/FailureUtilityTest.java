package org.jboss.sbomer.test.unit.sbom.service.core.utility;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.sbomer.events.common.FailureSpec;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationResult;
import org.jboss.sbomer.sbom.service.core.domain.exception.ValidationException;
import org.jboss.sbomer.sbom.service.core.utility.FailureUtility;
import org.junit.jupiter.api.Test;

class FailureUtilityTest {

    @Test
    void testBuildFailureSpecFromException_ValidationException() {
        ValidationException ex = new ValidationException("Invalid request parameters");
        
        FailureSpec failure = FailureUtility.buildFailureSpecFromException(ex);
        
        assertThat(failure.getErrorCode()).isEqualTo(ErrorResult.INVALID_REQUEST.name());
        assertThat(failure.getReason()).isEqualTo("Invalid request parameters");
        assertThat(failure.getDetails()).isNotNull();
        assertThat(failure.getDetails().get("exceptionClass")).isEqualTo(ValidationException.class.getName());
        assertThat(failure.getDetails().get("category")).isEqualTo("VALIDATION");
        assertThat(failure.getDetails().get("retryable")).isEqualTo("false");
        assertThat(failure.getDetails().get("ownership")).isEqualTo("CLIENT");
        assertThat(failure.getDetails().get("severity")).isEqualTo("WARN");
        assertThat(failure.getDetails().get("stackTrace")).isNotNull();
    }

    @Test
    void testBuildFailureSpecFromException_RuntimeException() {
        RuntimeException ex = new RuntimeException("Unexpected error occurred");
        
        FailureSpec failure = FailureUtility.buildFailureSpecFromException(ex);
        
        assertThat(failure.getErrorCode()).isEqualTo(ErrorResult.UNEXPECTED_ERROR.name());
        assertThat(failure.getReason()).isEqualTo("Unexpected error occurred");
        assertThat(failure.getDetails().get("exceptionClass")).isEqualTo(RuntimeException.class.getName());
        assertThat(failure.getDetails().get("category")).isEqualTo("INTERNAL");
        assertThat(failure.getDetails().get("retryable")).isEqualTo("false");
        assertThat(failure.getDetails().get("ownership")).isEqualTo("SERVICE");
        assertThat(failure.getDetails().get("severity")).isEqualTo("ERROR");
    }

    @Test
    void testBuildFailureSpecFromException_NullMessage() {
        RuntimeException ex = new RuntimeException();
        
        FailureSpec failure = FailureUtility.buildFailureSpecFromException(ex);
        
        assertThat(failure.getReason()).contains("An error occurred: RuntimeException");
    }

    @Test
    void testBuildFailureSpecFromGenerationFailure_WithUpstreamReason() {
        String generationId = "gen-123";
        String runId = "run-456";
        String targetIdentifier = "quay.io/example/image:latest";
        String upstreamReason = "TaskRunFailed: pod terminated unexpectedly";
        
        FailureSpec failure = FailureUtility.buildFailureSpecFromGenerationFailure(
            GenerationResult.ERR_GENERAL,
            targetIdentifier,
            upstreamReason,
            generationId,
            runId
        );
        
        assertThat(failure.getErrorCode()).isEqualTo(ErrorResult.GENERATOR_EXECUTION_FAILED.name());
        assertThat(failure.getReason())
            .contains("Generator failed")
            .contains(targetIdentifier)
            .contains(upstreamReason);
        
        assertThat(failure.getDetails()).isNotNull();
        assertThat(failure.getDetails().get("upstreamReason")).isEqualTo(upstreamReason);
        assertThat(failure.getDetails().get("legacyResultCode")).isEqualTo(String.valueOf(GenerationResult.ERR_GENERAL.getCode()));
        assertThat(failure.getDetails().get("legacyResultName")).isEqualTo("ERR_GENERAL");
        assertThat(failure.getDetails().get("category")).isEqualTo("EXTERNAL_EXECUTION");
        assertThat(failure.getDetails().get("retryable")).isEqualTo("true");
        assertThat(failure.getDetails().get("ownership")).isEqualTo("EXTERNAL_SYSTEM");
        assertThat(failure.getDetails().get("generationId")).isEqualTo(generationId);
        assertThat(failure.getDetails().get("runId")).isEqualTo(runId);
        assertThat(failure.getDetails().get("targetIdentifier")).isEqualTo(targetIdentifier);
    }

    @Test
    void testBuildFailureSpecFromGenerationFailure_WithoutUpstreamReason() {
        String targetIdentifier = "quay.io/example/image:latest";
        
        FailureSpec failure = FailureUtility.buildFailureSpecFromGenerationFailure(
            GenerationResult.ERR_CONFIG_INVALID,
            targetIdentifier,
            null,
            "gen-123",
            "run-456"
        );
        
        assertThat(failure.getErrorCode()).isEqualTo(ErrorResult.EXTERNAL_BAD_CONFIGURATION.name());
        assertThat(failure.getReason())
            .contains("Generator received invalid configuration")
            .contains(targetIdentifier);
        assertThat(failure.getDetails().get("upstreamReason")).isEmpty();
        assertThat(failure.getDetails().get("retryable")).isEqualTo("false");
    }

    @Test
    void testBuildFailureSpecFromGenerationFailure_SystemError() {
        FailureSpec failure = FailureUtility.buildFailureSpecFromGenerationFailure(
            GenerationResult.ERR_SYSTEM,
            "quay.io/example/image:latest",
            "System resource exhausted",
            "gen-123",
            "run-456"
        );
        
        assertThat(failure.getErrorCode()).isEqualTo(ErrorResult.EXTERNAL_SYSTEM_ERROR.name());
        assertThat(failure.getDetails().get("category")).isEqualTo("EXTERNAL_EXECUTION");
        assertThat(failure.getDetails().get("retryable")).isEqualTo("true");
        assertThat(failure.getDetails().get("severity")).isEqualTo("ERROR");
    }

    @Test
    void testBuildFailureSpecFromEnhancementFailure_WithUpstreamReason() {
        String enhancementId = "enh-789";
        String runId = "run-012";
        String enhancerName = "rpm-enhancer";
        String upstreamReason = "TaskRunFailed: timeout exceeded";
        
        FailureSpec failure = FailureUtility.buildFailureSpecFromEnhancementFailure(
            EnhancementResult.ERR_GENERAL,
            enhancerName,
            upstreamReason,
            enhancementId,
            runId
        );
        
        assertThat(failure.getErrorCode()).isEqualTo(ErrorResult.ENHANCER_EXECUTION_FAILED.name());
        assertThat(failure.getReason())
            .contains("Enhancer failed")
            .contains(enhancerName)
            .contains(upstreamReason);
        
        assertThat(failure.getDetails()).isNotNull();
        assertThat(failure.getDetails().get("upstreamReason")).isEqualTo(upstreamReason);
        assertThat(failure.getDetails().get("legacyResultCode")).isEqualTo(String.valueOf(EnhancementResult.ERR_GENERAL.getCode()));
        assertThat(failure.getDetails().get("legacyResultName")).isEqualTo("ERR_GENERAL");
        assertThat(failure.getDetails().get("category")).isEqualTo("EXTERNAL_EXECUTION");
        assertThat(failure.getDetails().get("retryable")).isEqualTo("true");
        assertThat(failure.getDetails().get("ownership")).isEqualTo("EXTERNAL_SYSTEM");
        assertThat(failure.getDetails().get("enhancementId")).isEqualTo(enhancementId);
        assertThat(failure.getDetails().get("runId")).isEqualTo(runId);
        assertThat(failure.getDetails().get("enhancerName")).isEqualTo(enhancerName);
    }

    @Test
    void testBuildFailureSpecFromEnhancementFailure_WithoutUpstreamReason() {
        String enhancerName = "rpm-enhancer";
        
        FailureSpec failure = FailureUtility.buildFailureSpecFromEnhancementFailure(
            EnhancementResult.ERR_CONFIG_INVALID,
            enhancerName,
            null,
            "enh-789",
            "run-012"
        );
        
        assertThat(failure.getErrorCode()).isEqualTo(ErrorResult.EXTERNAL_BAD_CONFIGURATION.name());
        assertThat(failure.getReason())
            .contains("Enhancer received invalid configuration")
            .contains(enhancerName);
        assertThat(failure.getDetails().get("upstreamReason")).isEmpty();
        assertThat(failure.getDetails().get("retryable")).isEqualTo("false");
    }

    @Test
    void testBuildFailureSpecFromEnhancementFailure_SystemError() {
        FailureSpec failure = FailureUtility.buildFailureSpecFromEnhancementFailure(
            EnhancementResult.ERR_ENHANCEMENT,
            "rpm-enhancer",
            "Database connection lost",
            "enh-789",
            "run-012"
        );
        
        assertThat(failure.getErrorCode()).isEqualTo(ErrorResult.ENHANCER_EXECUTION_FAILED.name());
        assertThat(failure.getDetails().get("category")).isEqualTo("EXTERNAL_EXECUTION");
        assertThat(failure.getDetails().get("retryable")).isEqualTo("true");
        assertThat(failure.getDetails().get("severity")).isEqualTo("ERROR");
    }

    @Test
    void testBuildFailureSpecFromGenerationFailure_NullContextIds() {
        FailureSpec failure = FailureUtility.buildFailureSpecFromGenerationFailure(
            GenerationResult.ERR_GENERAL,
            "quay.io/example/image:latest",
            "Error occurred",
            null,
            null
        );
        
        assertThat(failure.getDetails().get("generationId")).isNull();
        assertThat(failure.getDetails().get("runId")).isNull();
        assertThat(failure.getDetails().get("targetIdentifier")).isNotNull();
    }

    @Test
    void testBuildFailureSpecFromEnhancementFailure_NullContextIds() {
        FailureSpec failure = FailureUtility.buildFailureSpecFromEnhancementFailure(
            EnhancementResult.ERR_GENERAL,
            "rpm-enhancer",
            "Error occurred",
            null,
            null
        );
        
        assertThat(failure.getDetails().get("enhancementId")).isNull();
        assertThat(failure.getDetails().get("runId")).isNull();
        assertThat(failure.getDetails().get("enhancerName")).isNotNull();
    }

    @Test
    void testStackTraceTruncation() {
        // Create exception with deep stack trace
        Exception ex = createDeepStackTraceException(100);
        
        FailureSpec failure = FailureUtility.buildFailureSpecFromException(ex);
        
        String stackTrace = failure.getDetails().get("stackTrace");
        assertThat(stackTrace).isNotNull();
        
        // Stack trace should be limited to avoid excessive payload
        if (stackTrace.length() > 5000) {
            assertThat(stackTrace).contains("(truncated)");
        }
    }

    private Exception createDeepStackTraceException(int depth) {
        if (depth == 0) {
            return new RuntimeException("Deep stack trace");
        }
        try {
            throw createDeepStackTraceException(depth - 1);
        } catch (Exception e) {
            return e;
        }
    }
}
