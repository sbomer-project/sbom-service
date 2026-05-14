package org.jboss.sbomer.sbom.service.core.port.api;

import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;

/**
 * Port for managing execution runs and their state transitions.
 */
public interface RunManagement {
    
    /**
     * Complete a GenerationRun and trigger bottom-up roll-up.
     *
     * @param runId The ID of the run to complete
     * @param errorResult The canonical error result. <b>null indicates success</b>,
     *                    any ErrorResult value indicates failure.
     * @param message Optional human-readable message
     */
    void completeGenerationRun(String runId, ErrorResult errorResult, String message);
    
    /**
     * Complete an EnhancementRun and trigger bottom-up roll-up.
     *
     * @param runId The ID of the run to complete
     * @param errorResult The canonical error result. <b>null indicates success</b>,
     *                    any ErrorResult value indicates failure.
     * @param message Optional human-readable message
     */
    void completeEnhancementRun(String runId, ErrorResult errorResult, String message);
    
    /**
     * Retry a failed Generation with top-down resurrection.
     * Creates a new GenerationRun and updates parent Request status.
     * 
     * @param generationId The ID of the generation to retry
     * @return The newly created run record
     */
    GenerationRunRecord retryGeneration(String generationId);
    
    /**
     * Retry a failed Enhancement with top-down resurrection.
     * Creates a new EnhancementRun and updates parent Generation status.
     *
     * @param enhancementId The ID of the enhancement to retry
     * @return The newly created run record
     */
    EnhancementRunRecord retryEnhancement(String enhancementId);
    
    /**
     * Roll up Generation statuses to the parent Request.
     * Recalculates overall RequestStatus based on current generation states.
     *
     * @param requestId The ID of the request to update
     */
    void rollUpGenerationsToRequest(String requestId);
}
