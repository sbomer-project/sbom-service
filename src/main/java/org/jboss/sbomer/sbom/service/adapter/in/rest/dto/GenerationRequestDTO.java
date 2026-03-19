package org.jboss.sbomer.sbom.service.adapter.in.rest.dto;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a single generation task within a larger request.
 */
@Schema(description = "A wrapper for a single generation task definition.")
public record GenerationRequestDTO(
        @NotNull(message = "A target must be specified for each generation request")
        @Valid
        @Schema(description = "The artifact target configuration.", required = true)
        TargetDTO target,
        @Schema(description = "Optional map of key-value configuration options provided by the handler to pass specific flags or overrides.")
        Map<String, String> handlerProvidedOptions
) {}
