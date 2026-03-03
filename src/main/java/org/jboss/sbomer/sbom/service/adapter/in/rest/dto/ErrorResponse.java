package org.jboss.sbomer.sbom.service.adapter.in.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Standard error response structure returned by all exception mappers.
 */
@Schema(description = "Standard error response")
public record ErrorResponse(
    @Schema(description = "Error type", examples = {"Not Found", "Conflict", "Validation Error", "Internal Server Error"})
    String error,
    
    @Schema(description = "Detailed error message", examples = {"Generation with ID gen-123 not found", "Target cannot be null"})
    String message,
    
    @Schema(description = "HTTP status code", examples = {"404", "409", "400", "500"})
    int status,
    
    @Schema(description = "Timestamp of the error in ISO 8601 format", examples = {"2026-02-23T15:44:48.253Z"})
    String timestamp
) {}
