package org.jboss.sbomer.sbom.service.adapter.in.rest;

import java.time.Instant;

import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.ErrorResponse;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.utility.ErrorMapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic exception mapper for unhandled exceptions.
 * Returns HTTP 500 (Internal Server Error) with structured error response using result+reason+status pattern.
 */
@Provider
@Slf4j
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        ErrorResult result = ErrorMapper.fromException(exception);
        
        log.error("Unhandled exception in REST endpoint: result={} exception={} message={}", 
                  result, exception.getClass().getSimpleName(), exception.getMessage(), exception);
        
        // Build a safe reason message that doesn't leak internal details
        String reason = "An unexpected error occurred while processing your request";
        if (exception.getMessage() != null && !exception.getMessage().isEmpty()) {
            // Only include exception message if it's safe (not a stack trace or internal detail)
            if (!exception.getMessage().contains("\n") && exception.getMessage().length() < 200) {
                reason = exception.getMessage();
            }
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            result,
            reason,
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            result.getCategory(),
            null, // correlationId - could be extracted from context if available
            null, // generationId
            null, // enhancementId
            Instant.now().toString()
        );
        
        return Response.serverError()
                .entity(errorResponse)
                .build();
    }
}
