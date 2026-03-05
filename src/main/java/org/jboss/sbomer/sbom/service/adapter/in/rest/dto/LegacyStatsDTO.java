package org.jboss.sbomer.sbom.service.adapter.in.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

// Needed for some legacy enhancers that query information about SBOMer to include in the generated SBOMs
@Schema(name = "Stats", description = "Basic system information and statistics, LEGACY, used only by components relying on some SBOMer PreviousGen endpoints")
public record LegacyStatsDTO(
    @Schema(description = "The version of the SBOMer service")
    String version,

    @Schema(description = "The release environment or identifier (i.e. helm chart name)")
    String release
) {}
