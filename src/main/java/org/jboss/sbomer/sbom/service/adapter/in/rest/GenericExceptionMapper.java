package org.jboss.sbomer.sbom.service.adapter.in.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception e) {
        log.error("Unhandled exception in REST endpoint: {}", e.getMessage(), e);
        return Response.serverError().entity(e.getMessage()).build();
    }
}