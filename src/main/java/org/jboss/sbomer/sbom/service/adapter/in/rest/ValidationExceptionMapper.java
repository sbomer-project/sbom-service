package org.jboss.sbomer.sbom.service.adapter.in.rest;

import java.time.Instant;
import java.util.Map;

import org.jboss.sbomer.sbom.service.core.domain.exception.ValidationException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Exception mapper for ValidationException.
 * Returns HTTP 400 (Bad Request) with structured error response.
 */
@Provider
@Slf4j
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        log.warn("Validation error: {}", exception.getMessage());
        
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of(
                    "error", "Validation Error",
                    "message", exception.getMessage(),
                    "status", 400,
                    "timestamp", Instant.now().toString()
                ))
                .build();
    }
}

