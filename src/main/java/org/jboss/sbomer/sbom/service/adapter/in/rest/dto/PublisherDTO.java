package org.jboss.sbomer.sbom.service.adapter.in.rest.dto;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents a destination where the generated SBOM should be published.
 */
@Schema(description = "Configuration for an external system where the final SBOM should be sent (e.g., a message bus or storage bucket).")
public record PublisherDTO(
        @NotBlank(message = "Publisher name must be provided")
        @Schema(description = "The unique name of the publisher implementation.", examples = {"dependency-check-publisher"}, required = true)
        String name,

        @Schema(description = "The version of the publisher to use (optional).", examples = {"1.0.0"})
        String version,

        @Schema(description = "Arbitrary configuration options required by the specific publisher.", examples = {"{\"foo\": \"bar\", \"x\": \"y\"}"})
        Map<String, String> options
) {}
