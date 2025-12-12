package org.jboss.sbomer.sbom.service.adapter.in.rest.dto;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * The main request body for triggering an SBOM generation via the REST API.
 */
@Schema(name = "GenerationBatchRequest", description = "The root payload for triggering one or more SBOM generations.")
public record GenerationRequestsDTO(
        @NotEmpty(message = "At least one generation request must be provided")
        @Schema(description = "A list of artifacts to generate SBOMs for.", required = true)
        List<@Valid GenerationRequestDTO> generationRequests,

        @Schema(description = "Optional list of publishers to notify upon completion.")
        List<@Valid PublisherDTO> publishers
) {}
