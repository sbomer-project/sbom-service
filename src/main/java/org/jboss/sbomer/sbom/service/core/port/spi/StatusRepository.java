package org.jboss.sbomer.sbom.service.core.port.spi;

import java.util.List;

import org.jboss.sbomer.sbom.service.adapter.in.rest.model.Page;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;

/**
 * <p>
 * To store and fetch the status of an SBOM generation or enhancement
 * </p>
 */
public interface StatusRepository {

    /**
     * Saves a RequestRecord.
     */
    void saveRequestRecord(RequestRecord record);

    /**
     * Saves or updates a RequestRecord in the database.
     */
    void updateRequestRecord(RequestRecord record);

    /**
     * Finds a RequestRecord by its unique ID.
     */
    RequestRecord findRequestById(String requestId);

    /**
     * Finds all RequestRecords with pagination support.
     * @param pageIndex 0-based page index
     * @param pageSize number of records per page
     */
    Page<RequestRecord> findAllRequests(int pageIndex, int pageSize);

    /**
     * Saves a GenerationRecord.
     */
    void saveGeneration(GenerationRecord record);

    /**
     * Finds a GenerationRecord by its unique ID.
     */
    GenerationRecord findGenerationById(String generationId);

    /**
     * Finds all Generations with pagination support.
     */
    Page<GenerationRecord> findAllGenerations(int pageIndex, int pageSize);

    /**
     * Find generations by request ID
     */
    List<GenerationRecord> findGenerationsByRequestId(String requestId);

    /**
     * Find generations by request ID (Paginated and used by UI/Admin)
     */
    Page<GenerationRecord> findGenerationsByRequestId(String requestId, int pageIndex, int pageSize);

    /**
     * Finds a list of GenerationRecords with a specific status.
     * This is used by the scheduler to find new work.
     */
    List<GenerationRecord> findByGenerationStatus(GenerationStatus status);

    /**
     * Saves or updates a GenerationRecord in the database.
     */
    void updateGeneration(GenerationRecord record);
    /**
     * Saves a GenerationRecord.
     */
    void saveEnhancement(EnhancementRecord record);
    /**
     * Finds a EnhancementRecord by its unique ID.
     */
    EnhancementRecord findEnhancementById(String enhancementId);

    /**
     * Finds a list of EnhancementRecords with a specific status.
     * This is used by the scheduler to find new work.
     */
    List<EnhancementRecord> findByEnhancementStatus(EnhancementStatus status);


    /**
     * Find enhancements by generation ID
     */
    List<EnhancementRecord> findEnhancementsByGenerationId(String generationId);

    /**
     * Saves or updates a EnhancementRecord in the database.
     */
    void updateEnhancement(EnhancementRecord record);

    /**
     * Finds all Enhancements with pagination support.
     */
    Page<EnhancementRecord> findAllEnhancements(int pageIndex, int pageSize);

    /**
     * Check if all generations + enhancements are completed for a given generation id (specific generation)
     */
    boolean isGenerationAndEnhancementsFinished(String generationId);

    /**
     * Check if all generations + enhancements are completed for a given request id (ALL generations in a request)
     */
    boolean isAllGenerationRequestsFinished(String requestId);

    /**
     * For a completed GenerationRecord, return all final SBOM URLs for it. This means if it went through
     * enhancers, it should give the urls of the last enhancement step
     */
    List<String> getFinalSbomUrlsForCompletedGeneration(String generationId);
}
