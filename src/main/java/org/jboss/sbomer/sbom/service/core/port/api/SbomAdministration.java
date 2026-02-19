package org.jboss.sbomer.sbom.service.core.port.api;

import java.util.List;

import org.jboss.sbomer.sbom.service.adapter.in.rest.model.Page;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;

/**
 * Driving Port for SBOM Administration tasks.
 * <p>
 * This interface defines the use cases available to external drivers (like a REST API, CLI, or UI)
 * for managing, querying, and troubleshooting SBOM generations.
 * </p>
 */
public interface SbomAdministration {

    /**
     * Fetches a paginated list of high-level SBOM requests.
     *
     * @param pageIndex The 0-based page index.
     * @param pageSize  The number of records per page.
     * @return A page of RequestRecords.
     */
    Page<RequestRecord> fetchRequests(int pageIndex, int pageSize);

    /**
     * Retrieves a single SBOM request by its ID.
     *
     * @param requestId The unique ID of the request.
     * @return The RequestRecord, or null if not found.
     */
    RequestRecord getRequest(String requestId);

    /**
     * Fetches a paginated list of generations belonging to a specific request.
     *
     * @param requestId The ID of the parent request.
     * @param pageIndex The 0-based page index.
     * @param pageSize  The number of records per page.
     * @return A page of GenerationRecords.
     */
    Page<GenerationRecord> fetchGenerationsForRequest(String requestId, int pageIndex, int pageSize);


    /**
     * Fetches a paginated list of all generations.
     * @param pageIndex The 0-based page index.
     * @param pageSize The number of records per page.
     * @return A page of GenerationRecords.
     */
    Page<GenerationRecord> fetchGenerations(int pageIndex, int pageSize);

    /**
     * Retrieves a single generation by its ID.
     *
     * @param generationId The unique ID of the generation.
     * @return The GenerationRecord, or null if not found.
     */
    GenerationRecord getGeneration(String generationId);

    /**
     * Retrieves all generations for a request without pagination.
     * useful for internal tools or small-scale exports.
     *
     * @param requestId The ID of the parent request.
     * @return A list of all GenerationRecords for the request.
     */
    List<GenerationRecord> getGenerationsForRequest(String requestId);

    /**
     * Triggers a retry for a specific generation that is in a FAILED state.
     * <p>
     * This will reset the status to NEW and re-schedule the generation event.
     * </p>
     *
     * @param generationId The unique ID of the generation to retry.
     */
    void retryGeneration(String generationId);


    /**
     * Retrieves a single enhancement by its ID.
     * @param enhancementId The unique ID of the enhancement.
     * @return The EnhancementRecord, or null if not found.
     */
    EnhancementRecord getEnhancement(String enhancementId);

    /**
     * Retrieves all enhancements for a generation.
     * @param generationId The ID of the parent generation.
     * @return A list of all EnhancementRecords for the generation.
     */
    List<EnhancementRecord> getEnhancementsForGeneration(String generationId);


    /**
     * Fetches a paginated list of all enhancements.
     * @param pageIndex The 0-based page index.
     * @param pageSize The number of records per page.
     * @return A page of EnhancementRecords.
     */
    Page<EnhancementRecord> fetchEnhancements(int pageIndex, int pageSize);

    /**
     * Triggers a retry for a specific enhancement that is in a FAILED state.
     * <p>
     * This will reset the status to NEW and re-schedule the enhancement event,
     * using the output of the previous step as input.
     * </p>
     *
     * @param enhancementId The unique ID of the enhancement to retry.
     */
    void retryEnhancement(String enhancementId);



}
