package org.jboss.sbomer.sbom.service.adapter.in.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Response returned when generation is successfully triggered.
 */
@Schema(description = "Generation trigger response")
public record TriggerResponse(
    @Schema(description = "Batch request ID for tracking", examples = {"req-12345"})
    String id
) {}
