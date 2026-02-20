package org.jboss.sbomer.sbom.service.adapter.in.rest;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {

    @Override
    public Response toResponse(EntityNotFoundException e) {
        return Response.status(NOT_FOUND).entity(e.getMessage()).build();
    }
}
