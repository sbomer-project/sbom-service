package org.jboss.sbomer.sbom.service.adapter.in.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Response returned when a retry operation is successfully scheduled.
 */
@Schema(description = "Retry operation response")
public record RetryResponse(
    @Schema(description = "Success message", examples = {"Retry scheduled"})
    String message,
    
    @Schema(description = "ID of the entity being retried", examples = {"gen-12345", "enh-67890"})
    String id
) {}