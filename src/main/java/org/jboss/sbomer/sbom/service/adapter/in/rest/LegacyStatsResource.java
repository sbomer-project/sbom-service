package org.jboss.sbomer.sbom.service.adapter.in.rest;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.sbomer.sbom.service.adapter.in.rest.dto.LegacyStatsDTO;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint specifically designed to provide system information to components that need it. Imitates the
 * legacy stats endpoint of PreviousGen since some components rely on it
 */
@Path("/api/v1beta1/stats")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
public class LegacyStatsResource {

    @ConfigProperty(name = "sbomer.system.version", defaultValue = "undefined")
    String version;

    @ConfigProperty(name = "sbomer.system.release", defaultValue = "undefined")
    String release;

    @GET
    @Operation(summary = "Retrieve system info", description = "Provides basic version and release information.")
    @APIResponse(
        responseCode = "200",
        description = "System info retrieved successfully",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LegacyStatsDTO.class))
    )
    public Response getStats() {
        LegacyStatsDTO stats = new LegacyStatsDTO(version, release);
        return Response.ok(stats).build();
    }
}
