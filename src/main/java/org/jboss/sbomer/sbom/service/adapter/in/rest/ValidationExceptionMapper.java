package org.jboss.sbomer.sbom.service.adapter.in.rest;

import java.time.Instant;

import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.ErrorResponse;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.domain.exception.ValidationException;
import org.jboss.sbomer.sbom.service.core.utility.ErrorMapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Exception mapper for ValidationException.
 * Returns HTTP 400 (Bad Request) with structured error response using result+reason+status pattern.
 */
@Provider
@Slf4j
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        ErrorResult result = ErrorMapper.fromException(exception).orElse(ErrorResult.UNEXPECTED_ERROR);
        
        log.warn("Validation error: result={} reason={}", result, exception.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            result,
            exception.getMessage(),
            Response.Status.BAD_REQUEST.getStatusCode(),
            result.getCategory(),
            null, // correlationId - could be extracted from context if available
            null, // generationId
            null, // enhancementId
            Instant.now().toString()
        );
        
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
    }
}