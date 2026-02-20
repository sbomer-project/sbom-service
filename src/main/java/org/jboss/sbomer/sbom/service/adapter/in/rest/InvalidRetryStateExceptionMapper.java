package org.jboss.sbomer.sbom.service.adapter.in.rest;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;

import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidRetryStateExceptionMapper implements ExceptionMapper<InvalidRetryStateException> {

    @Override
    public Response toResponse(InvalidRetryStateException e) {
        return Response.status(CONFLICT).entity(e.getMessage()).build();
    }
}
