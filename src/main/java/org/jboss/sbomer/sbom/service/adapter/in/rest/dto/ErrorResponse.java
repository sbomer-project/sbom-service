package org.jboss.sbomer.sbom.service.adapter.in.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorCategory;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard error response structure using result+reason+status pattern.
 * 
 * This structure provides:
 * - result: Canonical service-owned error code
 * - reason: Human-readable explanation of what went wrong
 * - status: HTTP status code
 * - category: High-level error grouping
 * - Optional correlation and entity IDs for traceability
 */
@Schema(description = "Standard error response with result+reason+status structure")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @Schema(description = "Canonical error result code", examples = {"INVALID_STATE_TRANSITION", "GENERATOR_EXECUTION_FAILED", "ENTITY_NOT_FOUND"})
    ErrorResult result,
    
    @Schema(description = "Human-readable explanation of what went wrong", examples = {"Enhancement enh-123 cannot be retried because it is in COMPLETED state", "Generator failed to produce SBOM for target quay.io/example/image:latest"})
    String reason,
    
    @Schema(description = "HTTP status code", examples = {"404", "409", "400", "500"})
    int status,
    
    @Schema(description = "Error category for grouping", examples = {"VALIDATION", "EXTERNAL_EXECUTION", "ORCHESTRATION"})
    ErrorCategory category,
    
    @Schema(description = "Correlation ID for request tracing", examples = {"req-123"})
    String correlationId,
    
    @Schema(description = "Generation ID if applicable", examples = {"gen-456"})
    String generationId,
    
    @Schema(description = "Enhancement ID if applicable", examples = {"enh-789"})
    String enhancementId,
    
    @Schema(description = "Timestamp of the error in ISO 8601 format", examples = {"2026-05-04T10:00:00Z"})
    String timestamp
) {}