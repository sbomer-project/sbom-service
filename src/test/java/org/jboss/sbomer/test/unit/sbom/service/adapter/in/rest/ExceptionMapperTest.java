package org.jboss.sbomer.test.unit.sbom.service.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.jboss.sbomer.sbom.service.adapter.in.rest.EntityNotFoundExceptionMapper;
import org.jboss.sbomer.sbom.service.adapter.in.rest.GenericExceptionMapper;
import org.jboss.sbomer.sbom.service.adapter.in.rest.InvalidRetryStateExceptionMapper;
import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.ErrorResponse;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorCategory;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;
import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

/**
 * Unit tests for REST exception mappers.
 * Tests verify that exceptions are properly mapped to ErrorResponse with canonical error codes.
 */
class ExceptionMapperTest {

    @Test
    void testEntityNotFoundReturnsNotFound() {
        // Given
        EntityNotFoundExceptionMapper mapper = new EntityNotFoundExceptionMapper();
        String message = "Generation with ID gen-1 not found";
        EntityNotFoundException exception = new EntityNotFoundException(message);

        // When
        Response response = mapper.toResponse(exception);
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();

        // Then
        assertEquals(404, response.getStatus());
        assertEquals(ErrorResult.ENTITY_NOT_FOUND, errorResponse.result());
        assertEquals(message, errorResponse.reason());
        assertEquals(404, errorResponse.status());
        assertEquals(ErrorCategory.VALIDATION, errorResponse.category());
        assertNotNull(errorResponse.timestamp());
    }

    @Test
    void testInvalidRetryStateReturnsConflict() {
        // Given
        InvalidRetryStateExceptionMapper mapper = new InvalidRetryStateExceptionMapper();
        String message = "Cannot retry generation in status: GENERATING";
        InvalidRetryStateException exception = new InvalidRetryStateException(message);

        // When
        Response response = mapper.toResponse(exception);
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();

        // Then
        assertEquals(409, response.getStatus());
        assertEquals(ErrorResult.INVALID_STATE_TRANSITION, errorResponse.result());
        assertEquals(message, errorResponse.reason());
        assertEquals(409, errorResponse.status());
        assertEquals(ErrorCategory.VALIDATION, errorResponse.category());
        assertNotNull(errorResponse.timestamp());
    }

    @Test
    void testUnhandledExceptionReturnsInternalServerError() {
        // Given
        GenericExceptionMapper mapper = new GenericExceptionMapper();
        String message = "Unexpected error";
        RuntimeException exception = new RuntimeException(message);

        // When
        Response response = mapper.toResponse(exception);
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();

        // Then
        assertEquals(500, response.getStatus());
        assertEquals(ErrorResult.UNEXPECTED_ERROR, errorResponse.result());
        assertEquals(message, errorResponse.reason());
        assertEquals(500, errorResponse.status());
        assertEquals(ErrorCategory.INTERNAL, errorResponse.category());
        assertNotNull(errorResponse.timestamp());
    }

    @Test
    void testGenericExceptionMapperSanitizesLongMessages() {
        // Given
        GenericExceptionMapper mapper = new GenericExceptionMapper();
        String longMessage = "a".repeat(300); // Message longer than 200 chars
        RuntimeException exception = new RuntimeException(longMessage);

        // When
        Response response = mapper.toResponse(exception);
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();

        // Then
        assertEquals(500, response.getStatus());
        assertEquals(ErrorResult.UNEXPECTED_ERROR, errorResponse.result());
        // Should use generic message instead of the long one
        assertEquals("An unexpected error occurred while processing your request", errorResponse.reason());
    }

    @Test
    void testGenericExceptionMapperSanitizesMultilineMessages() {
        // Given
        GenericExceptionMapper mapper = new GenericExceptionMapper();
        String multilineMessage = "Error\nwith\nstack\ntrace";
        RuntimeException exception = new RuntimeException(multilineMessage);

        // When
        Response response = mapper.toResponse(exception);
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();

        // Then
        assertEquals(500, response.getStatus());
        assertEquals(ErrorResult.UNEXPECTED_ERROR, errorResponse.result());
        // Should use generic message instead of multiline one
        assertEquals("An unexpected error occurred while processing your request", errorResponse.reason());
    }

    @Test
    void testGenericExceptionMapperHandlesNullMessage() {
        // Given
        GenericExceptionMapper mapper = new GenericExceptionMapper();
        RuntimeException exception = new RuntimeException((String) null);

        // When
        Response response = mapper.toResponse(exception);
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();

        // Then
        assertEquals(500, response.getStatus());
        assertEquals(ErrorResult.UNEXPECTED_ERROR, errorResponse.result());
        assertEquals("An unexpected error occurred while processing your request", errorResponse.reason());
    }

    @Test
    void testGenericExceptionMapperHandlesEmptyMessage() {
        // Given
        GenericExceptionMapper mapper = new GenericExceptionMapper();
        RuntimeException exception = new RuntimeException("");

        // When
        Response response = mapper.toResponse(exception);
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();

        // Then
        assertEquals(500, response.getStatus());
        assertEquals(ErrorResult.UNEXPECTED_ERROR, errorResponse.result());
        assertEquals("An unexpected error occurred while processing your request", errorResponse.reason());
    }
}