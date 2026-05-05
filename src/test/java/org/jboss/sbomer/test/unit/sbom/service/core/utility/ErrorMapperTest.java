package org.jboss.sbomer.test.unit.sbom.service.core.utility;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorCategory;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorOwnership;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorSeverity;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationResult;
import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;
import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;
import org.jboss.sbomer.sbom.service.core.domain.exception.ValidationException;
import org.jboss.sbomer.sbom.service.core.utility.ErrorMapper;
import org.junit.jupiter.api.Test;

class ErrorMapperTest {

    @Test
    void testFromException_ValidationException() {
        ValidationException ex = new ValidationException("Invalid input");
        
        ErrorResult result = ErrorMapper.fromException(ex);
        
        assertThat(result).isEqualTo(ErrorResult.INVALID_REQUEST);
        assertThat(result.getCategory()).isEqualTo(ErrorCategory.VALIDATION);
        assertThat(result.isRetryable()).isFalse();
        assertThat(result.getOwnership()).isEqualTo(ErrorOwnership.CLIENT);
        assertThat(result.getSeverity()).isEqualTo(ErrorSeverity.WARN);
    }

    @Test
    void testFromException_EntityNotFoundException() {
        EntityNotFoundException ex = new EntityNotFoundException("Entity not found");
        
        ErrorResult result = ErrorMapper.fromException(ex);
        
        assertThat(result).isEqualTo(ErrorResult.ENTITY_NOT_FOUND);
        assertThat(result.getCategory()).isEqualTo(ErrorCategory.VALIDATION);
        assertThat(result.isRetryable()).isFalse();
        assertThat(result.getOwnership()).isEqualTo(ErrorOwnership.CLIENT);
    }

    @Test
    void testFromException_InvalidRetryStateException() {
        InvalidRetryStateException ex = new InvalidRetryStateException("Cannot retry");
        
        ErrorResult result = ErrorMapper.fromException(ex);
        
        assertThat(result).isEqualTo(ErrorResult.INVALID_STATE_TRANSITION);
        assertThat(result.getCategory()).isEqualTo(ErrorCategory.VALIDATION);
        assertThat(result.isRetryable()).isFalse();
    }

    @Test
    void testFromException_GenericException() {
        RuntimeException ex = new RuntimeException("Unexpected error");
        
        ErrorResult result = ErrorMapper.fromException(ex);
        
        assertThat(result).isEqualTo(ErrorResult.UNEXPECTED_ERROR);
        assertThat(result.getCategory()).isEqualTo(ErrorCategory.INTERNAL);
        assertThat(result.isRetryable()).isFalse();
        assertThat(result.getOwnership()).isEqualTo(ErrorOwnership.SERVICE);
        assertThat(result.getSeverity()).isEqualTo(ErrorSeverity.ERROR);
    }

    @Test
    void testFromGenerationResult_Success() {
        ErrorResult result = ErrorMapper.fromGenerationResult(GenerationResult.SUCCESS);
        
        assertThat(result).isNull(); // SUCCESS is not an error
    }

    @Test
    void testFromGenerationResult_GeneralError() {
        ErrorResult result = ErrorMapper.fromGenerationResult(GenerationResult.ERR_GENERAL);
        
        assertThat(result).isEqualTo(ErrorResult.GENERATOR_EXECUTION_FAILED);
        assertThat(result.getCategory()).isEqualTo(ErrorCategory.EXTERNAL_EXECUTION);
        assertThat(result.isRetryable()).isTrue();
        assertThat(result.getOwnership()).isEqualTo(ErrorOwnership.EXTERNAL_SYSTEM);
    }

    @Test
    void testFromGenerationResult_ConfigError() {
        ErrorResult result = ErrorMapper.fromGenerationResult(GenerationResult.ERR_CONFIG_INVALID);
        
        assertThat(result).isEqualTo(ErrorResult.EXTERNAL_BAD_CONFIGURATION);
        assertThat(result.getCategory()).isEqualTo(ErrorCategory.EXTERNAL_EXECUTION);
        assertThat(result.isRetryable()).isFalse();
        assertThat(result.getOwnership()).isEqualTo(ErrorOwnership.EXTERNAL_SYSTEM);
    }

    @Test
    void testFromGenerationResult_SystemError() {
        ErrorResult result = ErrorMapper.fromGenerationResult(GenerationResult.ERR_SYSTEM);
        
        assertThat(result).isEqualTo(ErrorResult.EXTERNAL_SYSTEM_ERROR);
        assertThat(result.getCategory()).isEqualTo(ErrorCategory.EXTERNAL_EXECUTION);
        assertThat(result.isRetryable()).isTrue();
    }

    @Test
    void testFromEnhancementResult_Success() {
        ErrorResult result = ErrorMapper.fromEnhancementResult(EnhancementResult.SUCCESS);
        
        assertThat(result).isNull(); // SUCCESS is not an error
    }

    @Test
    void testFromEnhancementResult_GeneralError() {
        ErrorResult result = ErrorMapper.fromEnhancementResult(EnhancementResult.ERR_GENERAL);
        
        assertThat(result).isEqualTo(ErrorResult.ENHANCER_EXECUTION_FAILED);
        assertThat(result.getCategory()).isEqualTo(ErrorCategory.EXTERNAL_EXECUTION);
        assertThat(result.isRetryable()).isTrue();
        assertThat(result.getOwnership()).isEqualTo(ErrorOwnership.EXTERNAL_SYSTEM);
    }

    @Test
    void testFromEnhancementResult_ConfigError() {
        ErrorResult result = ErrorMapper.fromEnhancementResult(EnhancementResult.ERR_CONFIG_INVALID);
        
        assertThat(result).isEqualTo(ErrorResult.EXTERNAL_BAD_CONFIGURATION);
        assertThat(result.getCategory()).isEqualTo(ErrorCategory.EXTERNAL_EXECUTION);
        assertThat(result.isRetryable()).isFalse();
        assertThat(result.getOwnership()).isEqualTo(ErrorOwnership.EXTERNAL_SYSTEM);
    }

    @Test
    void testFromEnhancementResult_EnhancementError() {
        ErrorResult result = ErrorMapper.fromEnhancementResult(EnhancementResult.ERR_ENHANCEMENT);
        
        assertThat(result).isEqualTo(ErrorResult.ENHANCER_EXECUTION_FAILED);
        assertThat(result.getCategory()).isEqualTo(ErrorCategory.EXTERNAL_EXECUTION);
        assertThat(result.isRetryable()).isTrue();
    }

    @Test
    void testBuildGenerationFailureReason_WithUpstreamReason() {
        String reason = ErrorMapper.buildGenerationFailureReason(
            GenerationResult.ERR_GENERAL,
            "quay.io/example/image:latest",
            "TaskRunFailed: pod terminated"
        );
        
        assertThat(reason)
            .contains("Generator failed")
            .contains("quay.io/example/image:latest")
            .contains("TaskRunFailed: pod terminated");
    }

    @Test
    void testBuildGenerationFailureReason_WithoutUpstreamReason() {
        String reason = ErrorMapper.buildGenerationFailureReason(
            GenerationResult.ERR_GENERAL,
            "quay.io/example/image:latest",
            null
        );
        
        assertThat(reason)
            .contains("Generator failed")
            .contains("quay.io/example/image:latest")
            .doesNotContain("null");
    }

    @Test
    void testBuildEnhancementFailureReason_WithUpstreamReason() {
        String reason = ErrorMapper.buildEnhancementFailureReason(
            EnhancementResult.ERR_GENERAL,
            "rpm-enhancer",
            "TaskRunFailed: timeout exceeded"
        );
        
        assertThat(reason)
            .contains("Enhancer failed")
            .contains("rpm-enhancer")
            .contains("TaskRunFailed: timeout exceeded");
    }

    @Test
    void testBuildEnhancementFailureReason_WithoutUpstreamReason() {
        String reason = ErrorMapper.buildEnhancementFailureReason(
            EnhancementResult.ERR_GENERAL,
            "rpm-enhancer",
            null
        );
        
        assertThat(reason)
            .contains("Enhancer failed")
            .contains("rpm-enhancer")
            .doesNotContain("null");
    }

    @Test
    void testErrorResultMetadata_Retryability() {
        // Retryable errors
        assertThat(ErrorResult.GENERATOR_EXECUTION_FAILED.isRetryable()).isTrue();
        assertThat(ErrorResult.ENHANCER_EXECUTION_FAILED.isRetryable()).isTrue();
        assertThat(ErrorResult.EXTERNAL_SYSTEM_ERROR.isRetryable()).isTrue();
        assertThat(ErrorResult.EXTERNAL_TIMEOUT.isRetryable()).isTrue();
        
        // Non-retryable errors
        assertThat(ErrorResult.INVALID_REQUEST.isRetryable()).isFalse();
        assertThat(ErrorResult.EXTERNAL_BAD_CONFIGURATION.isRetryable()).isFalse();
        assertThat(ErrorResult.ENTITY_NOT_FOUND.isRetryable()).isFalse();
        assertThat(ErrorResult.INVALID_TARGET.isRetryable()).isFalse();
    }

    @Test
    void testErrorResultMetadata_Ownership() {
        // Client ownership
        assertThat(ErrorResult.INVALID_REQUEST.getOwnership()).isEqualTo(ErrorOwnership.CLIENT);
        assertThat(ErrorResult.ENTITY_NOT_FOUND.getOwnership()).isEqualTo(ErrorOwnership.CLIENT);
        
        // Service ownership
        assertThat(ErrorResult.CONFIG_INVALID.getOwnership()).isEqualTo(ErrorOwnership.SERVICE);
        assertThat(ErrorResult.UNEXPECTED_ERROR.getOwnership()).isEqualTo(ErrorOwnership.SERVICE);
        
        // External ownership
        assertThat(ErrorResult.GENERATOR_EXECUTION_FAILED.getOwnership()).isEqualTo(ErrorOwnership.EXTERNAL_SYSTEM);
        assertThat(ErrorResult.ENHANCER_EXECUTION_FAILED.getOwnership()).isEqualTo(ErrorOwnership.EXTERNAL_SYSTEM);
    }

    @Test
    void testErrorResultMetadata_Severity() {
        // Error level
        assertThat(ErrorResult.UNEXPECTED_ERROR.getSeverity()).isEqualTo(ErrorSeverity.ERROR);
        assertThat(ErrorResult.DATABASE_ERROR.getSeverity()).isEqualTo(ErrorSeverity.ERROR);
        assertThat(ErrorResult.GENERATOR_EXECUTION_FAILED.getSeverity()).isEqualTo(ErrorSeverity.ERROR);
        
        // Warning level
        assertThat(ErrorResult.INVALID_REQUEST.getSeverity()).isEqualTo(ErrorSeverity.WARN);
        assertThat(ErrorResult.EXTERNAL_TIMEOUT.getSeverity()).isEqualTo(ErrorSeverity.ERROR);
    }
}