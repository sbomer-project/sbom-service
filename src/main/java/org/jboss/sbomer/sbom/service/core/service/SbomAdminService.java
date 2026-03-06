package org.jboss.sbomer.sbom.service.core.service;

import java.util.List;

import org.jboss.sbomer.events.common.GenerationRequestSpec;
import org.jboss.sbomer.events.orchestration.EnhancementCreated;
import org.jboss.sbomer.events.orchestration.GenerationCreated;
import org.jboss.sbomer.sbom.service.adapter.in.rest.model.Page;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;
import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;
import org.jboss.sbomer.sbom.service.core.port.api.RunManagement;
import org.jboss.sbomer.sbom.service.core.port.api.SbomAdministration;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;
import org.jboss.sbomer.sbom.service.core.port.spi.enhancement.EnhancementScheduler;
import org.jboss.sbomer.sbom.service.core.port.spi.generation.GenerationScheduler;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class SbomAdminService implements SbomAdministration {

    StatusRepository statusRepository;
    GenerationScheduler generationScheduler;
    EnhancementScheduler enhancementScheduler;
    SbomMapper sbomMapper;
    RunManagement runManagement;

    @Inject
    public SbomAdminService(StatusRepository statusRepository, GenerationScheduler generationScheduler,
            EnhancementScheduler enhancementScheduler, SbomMapper sbomMapper, RunManagement runManagement) {
        this.statusRepository = statusRepository;
        this.generationScheduler = generationScheduler;
        this.enhancementScheduler = enhancementScheduler;
        this.sbomMapper = sbomMapper;
        this.runManagement = runManagement;
    }

    // --- READ OPERATIONS (Pass-through to Repository) ---

    @Override
    public Page<RequestRecord> fetchRequests(int pageIndex, int pageSize) {
        return statusRepository.findAllRequests(pageIndex, pageSize);
    }

    @Override
    public RequestRecord getRequest(String requestId) {
        return statusRepository.findRequestById(requestId);
    }

    @Override
    public Page<GenerationRecord> fetchGenerationsForRequest(String requestId, int pageIndex, int pageSize) {
        return statusRepository.findGenerationsByRequestId(requestId, pageIndex, pageSize);
    }

    @Override
    public Page<GenerationRecord> fetchGenerations(int pageIndex, int pageSize) {
        return statusRepository.findAllGenerations(pageIndex, pageSize);
    }

    @Override
    public GenerationRecord getGeneration(String generationId) {
        return statusRepository.findGenerationById(generationId);
    }

    @Override
    public List<GenerationRecord> getGenerationsForRequest(String requestId) {
        return statusRepository.findGenerationsByRequestId(requestId);
    }

    // --- WRITE OPERATIONS (Commands) ---

    @WithSpan
    public void retryGeneration(@SpanAttribute("generation.id") String generationId) {
        log.info("Retrying generation: {}", generationId);

        // 1. Use RunManagement to create new Run and resurrect the Generation
        // This handles: creating new Run, updating Generation status, reverse roll-up to Request
        GenerationRunRecord newRun = runManagement.retryGeneration(generationId);
        
        log.info("Created new GenerationRun for retry: runId={}, attempt={}",
                newRun.getId(), newRun.getAttemptNumber());

        // 2. Fetch the updated Generation record
        GenerationRecord record = statusRepository.findGenerationById(generationId);

        // 3. Reconstruct Context and schedule the retry
        GenerationRequestSpec originalSpec = sbomMapper.toGenerationRequestSpec(record);
        String retryCorrelationId = record.getRequestId();

        // 4. Build & Schedule Event
        GenerationCreated retryEvent = sbomMapper.toGenerationCreatedEvent(record, originalSpec, retryCorrelationId);
        generationScheduler.schedule(retryEvent);
        
        log.info("Successfully scheduled retry for generation: {}", generationId);
    }

    @Override
    public List<EnhancementRecord> getEnhancementsForGeneration(String generationId) {
        return statusRepository.findEnhancementsByGenerationId(generationId);
    }

    @Override
    public Page<EnhancementRecord> fetchEnhancements(int pageIndex, int pageSize) {
        return statusRepository.findAllEnhancements(pageIndex, pageSize);
    }

    @Override
    public EnhancementRecord getEnhancement(String enhancementId) {
        return statusRepository.findEnhancementById(enhancementId);
    }

    @WithSpan
    public void retryEnhancement(@SpanAttribute("enhancement.id") String enhancementId) {
        log.info("Retrying enhancement: {}", enhancementId);

        // 1. Fetch the Enhancement to get parent Generation ID
        EnhancementRecord record = statusRepository.findEnhancementById(enhancementId);
        if (record == null) {
            throw new EntityNotFoundException("Enhancement with ID " + enhancementId + " not found");
        }

        GenerationRecord parentGeneration = statusRepository.findGenerationById(record.getGenerationId());
        if (parentGeneration == null) {
            throw new InvalidRetryStateException("Cannot retry enhancement because parent generation is missing.");
        }

        // 2. Use RunManagement to create new Run and resurrect the Enhancement
        // This handles: creating new Run, updating Enhancement status, reverse roll-up to Generation/Request
        EnhancementRunRecord newRun = runManagement.retryEnhancement(enhancementId);
        
        log.info("Created new EnhancementRun for retry: runId={}, attempt={}",
                newRun.getId(), newRun.getAttemptNumber());

        // 3. Fetch the updated Enhancement record
        record = statusRepository.findEnhancementById(enhancementId);

        // 4. Determine Inputs
        EnhancementRecord lastFinished = findPreviousEnhancement(parentGeneration, record.getIndex());

        // 5. Build & Schedule Event
        EnhancementCreated retryEvent = sbomMapper.toEnhancementCreatedEvent(record, lastFinished, parentGeneration);
        enhancementScheduler.schedule(retryEvent);
        
        log.info("Successfully scheduled retry for enhancement: {}", enhancementId);
    }

    /**
     * Helper to find the enhancement that ran immediately before the target index.
     */
    private EnhancementRecord findPreviousEnhancement(GenerationRecord parent, int targetIndex) {
        if (targetIndex == 0) {
            return null;
        }

        return parent.getEnhancements().stream()
                .filter(e -> e.getIndex() == targetIndex - 1)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Could not find previous enhancement with index " + (targetIndex - 1)));
    }

}
