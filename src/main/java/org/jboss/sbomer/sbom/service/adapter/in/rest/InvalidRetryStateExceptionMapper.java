package org.jboss.sbomer.sbom.service.adapter.in.rest;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;

import java.time.Instant;
import java.util.Map;

import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class InvalidRetryStateExceptionMapper implements ExceptionMapper<InvalidRetryStateException> {

    @Override
    public Response toResponse(InvalidRetryStateException e) {
        log.warn("Invalid retry state: {}", e.getMessage());
        
        return Response.status(CONFLICT)
                .entity(Map.of(
                    "error", "Conflict",
                    "message", e.getMessage(),
                    "status", 409,
                    "timestamp", Instant.now().toString()
                ))
                .build();
    }
}
