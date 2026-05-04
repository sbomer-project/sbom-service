package org.jboss.sbomer.sbom.service.adapter.in.rest;

import java.time.Instant;

import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.ErrorResponse;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;
import org.jboss.sbomer.sbom.service.core.utility.ErrorMapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Exception mapper for InvalidRetryStateException.
 * Returns HTTP 409 (Conflict) with structured error response using result+reason+status pattern.
 */
@Provider
@Slf4j
public class InvalidRetryStateExceptionMapper implements ExceptionMapper<InvalidRetryStateException> {

    @Override
    public Response toResponse(InvalidRetryStateException exception) {
        ErrorResult result = ErrorMapper.fromException(exception);
        
        log.warn("Invalid retry state: result={} reason={}", result, exception.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            result,
            exception.getMessage(),
            Response.Status.CONFLICT.getStatusCode(),
            result.getCategory(),
            null, // correlationId - could be extracted from context if available
            null, // generationId - could be extracted from exception if available
            null, // enhancementId - could be extracted from exception if available
            Instant.now().toString()
        );
        
        return Response.status(Response.Status.CONFLICT)
                .entity(errorResponse)
                .build();
    }
}