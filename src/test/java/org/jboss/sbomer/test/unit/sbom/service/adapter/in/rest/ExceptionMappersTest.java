package org.jboss.sbomer.test.unit.sbom.service.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.sbomer.sbom.service.adapter.in.rest.EntityNotFoundExceptionMapper;
import org.jboss.sbomer.sbom.service.adapter.in.rest.GenericExceptionMapper;
import org.jboss.sbomer.sbom.service.adapter.in.rest.InvalidRetryStateExceptionMapper;
import org.jboss.sbomer.sbom.service.adapter.in.rest.ValidationExceptionMapper;
import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.ErrorResponse;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorCategory;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;
import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;
import org.jboss.sbomer.sbom.service.core.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

class ExceptionMappersTest {

    @Test
    void testValidationExceptionMapper() {
        ValidationExceptionMapper mapper = new ValidationExceptionMapper();
        ValidationException exception = new ValidationException("Invalid input parameter");
        
        Response response = mapper.toResponse(exception);
        
        assertThat(response.getStatus()).isEqualTo(400);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.result()).isEqualTo(ErrorResult.INVALID_REQUEST);
        assertThat(errorResponse.reason()).isEqualTo("Invalid input parameter");
        assertThat(errorResponse.status()).isEqualTo(400);
        assertThat(errorResponse.category()).isEqualTo(ErrorCategory.VALIDATION);
    }

    @Test
    void testEntityNotFoundExceptionMapper() {
        EntityNotFoundExceptionMapper mapper = new EntityNotFoundExceptionMapper();
        EntityNotFoundException exception = new EntityNotFoundException("Generation not found: gen-123");
        
        Response response = mapper.toResponse(exception);
        
        assertThat(response.getStatus()).isEqualTo(404);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.result()).isEqualTo(ErrorResult.ENTITY_NOT_FOUND);
        assertThat(errorResponse.reason()).isEqualTo("Generation not found: gen-123");
        assertThat(errorResponse.status()).isEqualTo(404);
        assertThat(errorResponse.category()).isEqualTo(ErrorCategory.VALIDATION);
    }

    @Test
    void testInvalidRetryStateExceptionMapper() {
        InvalidRetryStateExceptionMapper mapper = new InvalidRetryStateExceptionMapper();
        InvalidRetryStateException exception = new InvalidRetryStateException("Cannot retry: already in progress");
        
        Response response = mapper.toResponse(exception);
        
        assertThat(response.getStatus()).isEqualTo(409);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.result()).isEqualTo(ErrorResult.INVALID_STATE_TRANSITION);
        assertThat(errorResponse.reason()).isEqualTo("Cannot retry: already in progress");
        assertThat(errorResponse.status()).isEqualTo(409);
        assertThat(errorResponse.category()).isEqualTo(ErrorCategory.VALIDATION);
    }

    @Test
    void testGenericExceptionMapper_RuntimeException() {
        GenericExceptionMapper mapper = new GenericExceptionMapper();
        RuntimeException exception = new RuntimeException("Unexpected system error");
        
        Response response = mapper.toResponse(exception);
        
        assertThat(response.getStatus()).isEqualTo(500);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.result()).isEqualTo(ErrorResult.UNEXPECTED_ERROR);
        assertThat(errorResponse.reason()).isEqualTo("Unexpected system error");
        assertThat(errorResponse.status()).isEqualTo(500);
        assertThat(errorResponse.category()).isEqualTo(ErrorCategory.INTERNAL);
    }

    @Test
    void testGenericExceptionMapper_NullPointerException() {
        GenericExceptionMapper mapper = new GenericExceptionMapper();
        NullPointerException exception = new NullPointerException("Null value encountered");
        
        Response response = mapper.toResponse(exception);
        
        assertThat(response.getStatus()).isEqualTo(500);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.result()).isEqualTo(ErrorResult.UNEXPECTED_ERROR);
        assertThat(errorResponse.reason()).isEqualTo("Null value encountered");
    }

    @Test
    void testErrorResponseStructure() {
        ValidationExceptionMapper mapper = new ValidationExceptionMapper();
        ValidationException exception = new ValidationException("Test validation error");
        
        Response response = mapper.toResponse(exception);
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        
        // Verify result+reason+status structure
        assertThat(errorResponse.result()).isNotNull();
        assertThat(errorResponse.reason()).isNotNull();
        assertThat(errorResponse.status()).isNotZero();
        assertThat(errorResponse.category()).isNotNull();
    }

    @Test
    void testErrorResponseMetadata_ValidationError() {
        ValidationExceptionMapper mapper = new ValidationExceptionMapper();
        ValidationException exception = new ValidationException("Invalid parameter");
        
        Response response = mapper.toResponse(exception);
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        
        assertThat(errorResponse.category()).isEqualTo(ErrorCategory.VALIDATION);
        assertThat(errorResponse.result().isRetryable()).isFalse();
        assertThat(errorResponse.result().getOwnership().name()).isEqualTo("CLIENT");
        assertThat(errorResponse.result().getSeverity().name()).isEqualTo("WARN");
    }

    @Test
    void testErrorResponseMetadata_InternalError() {
        GenericExceptionMapper mapper = new GenericExceptionMapper();
        RuntimeException exception = new RuntimeException("System failure");
        
        Response response = mapper.toResponse(exception);
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        
        assertThat(errorResponse.category()).isEqualTo(ErrorCategory.INTERNAL);
        assertThat(errorResponse.result().isRetryable()).isFalse();
        assertThat(errorResponse.result().getOwnership().name()).isEqualTo("SERVICE");
        assertThat(errorResponse.result().getSeverity().name()).isEqualTo("ERROR");
    }
}
