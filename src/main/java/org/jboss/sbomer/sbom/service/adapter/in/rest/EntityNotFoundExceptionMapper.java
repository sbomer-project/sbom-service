package org.jboss.sbomer.sbom.service.adapter.in.rest;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

import java.time.Instant;
import java.util.Map;

import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {

    @Override
    public Response toResponse(EntityNotFoundException e) {
        log.warn("Entity not found: {}", e.getMessage());
        
        return Response.status(NOT_FOUND)
                .entity(Map.of(
                    "error", "Not Found",
                    "message", e.getMessage(),
                    "status", 404,
                    "timestamp", Instant.now().toString()
                ))
                .build();
    }
}
