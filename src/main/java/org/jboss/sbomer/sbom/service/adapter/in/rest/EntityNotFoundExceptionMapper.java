package org.jboss.sbomer.sbom.service.adapter.in.rest;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

import java.time.Instant;

import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.ErrorResponse;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;
import org.jboss.sbomer.sbom.service.core.utility.ErrorMapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Exception mapper for EntityNotFoundException.
 * Returns HTTP 404 (Not Found) with structured error response using result+reason+status pattern.
 */
@Provider
@Slf4j
public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {

    @Override
    public Response toResponse(EntityNotFoundException exception) {
        ErrorResult result = ErrorMapper.fromException(exception);
        
        log.warn("Entity not found: result={} reason={}", result, exception.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            result,
            exception.getMessage(),
            NOT_FOUND.getStatusCode(),
            result.getCategory(),
            null, // correlationId - could be extracted from context if available
            null, // generationId
            null, // enhancementId
            Instant.now().toString()
        );
        
        return Response.status(NOT_FOUND)
                .entity(errorResponse)
                .build();
    }
}