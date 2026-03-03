package org.jboss.sbomer.sbom.service.adapter.in.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents the target for an SBOM generation request from an end-user.
 */
@Schema(description = "Defines the specific artifact or source to analyze (e.g., a Container Image, Git Repository, RPM).")
public record TargetDTO(
        @NotBlank(message = "Target type must be provided")
        @JsonProperty("type")
        @Schema(description = "The type of the target artifact.", examples = {"CONTAINER_IMAGE", "RPM"}, required = true)
        String type,

        @NotBlank(message = "Target identifier must be provided")
        @JsonProperty("identifier")
        @Schema(description = "The specific identifier/coordinates for the target.", examples = {"quay.io/pct-security/mequal:latest"}, required = true)
        String identifier
) {}
