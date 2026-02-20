package org.jboss.sbomer.sbom.service.core.service;

import java.time.Instant;
import java.util.List;

import org.jboss.sbomer.events.common.GenerationRequestSpec;
import org.jboss.sbomer.events.orchestration.EnhancementCreated;
import org.jboss.sbomer.events.orchestration.GenerationCreated;
import org.jboss.sbomer.sbom.service.adapter.in.rest.model.Page;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;
import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;
import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;
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

    @Inject
    public SbomAdminService(StatusRepository statusRepository, GenerationScheduler generationScheduler,
            EnhancementScheduler enhancementScheduler, SbomMapper sbomMapper) {
        this.statusRepository = statusRepository;
        this.generationScheduler = generationScheduler;
        this.enhancementScheduler = enhancementScheduler;
        this.sbomMapper = sbomMapper;
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
        // Repository implementation handles locking/transaction here
        GenerationRecord record = statusRepository.findGenerationById(generationId);

        if (record == null) {
            throw new EntityNotFoundException("Generation with ID " + generationId + " not found");
        }

        if (GenerationStatus.FAILED != record.getStatus()) {
            throw new InvalidRetryStateException("Cannot retry generation in status: " + record.getStatus()
                    + ". Only FAILED generations can be retried.");
        }

        log.info("Retrying generation: {}", generationId);

        // 1. Reset the status
        record.setStatus(GenerationStatus.NEW);
        record.setReason(null);
        record.setResult(null);
        record.setFinished(null);
        record.setUpdated(Instant.now());

        // 2. Save to DB (Transaction/Lock handled by Adapter)
        statusRepository.updateGeneration(record);

        // 3. Reconstruct Context
        GenerationRequestSpec originalSpec = sbomMapper.toGenerationRequestSpec(record);
        String retryCorrelationId = record.getRequestId();

        // 4. Build & Schedule Event
        GenerationCreated retryEvent = sbomMapper.toGenerationCreatedEvent(record, originalSpec, retryCorrelationId);
        generationScheduler.schedule(retryEvent);
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
        // Repository implementation handles locking/transaction here
        EnhancementRecord record = statusRepository.findEnhancementById(enhancementId);

        if (record == null) {
            throw new EntityNotFoundException("Enhancement with ID " + enhancementId + " not found");
        }

        if (EnhancementStatus.FAILED != record.getStatus()) {
            throw new InvalidRetryStateException("Cannot retry enhancement in status: " + record.getStatus()
                    + ". Only FAILED enhancements can be retried.");
        }

        GenerationRecord parentGeneration = statusRepository.findGenerationById(record.getGenerationId());
        if (parentGeneration == null) {
            throw new InvalidRetryStateException("Cannot retry enhancement because parent generation is missing.");
        }

        log.info("Retrying enhancement: {}", enhancementId);

        // 1. Reset the status
        record.setStatus(EnhancementStatus.NEW);
        record.setReason(null);
        record.setResult(null);
        record.setFinished(null);
        record.setUpdated(Instant.now());

        // 2. Save to DB (Transaction/Lock handled by Adapter)
        statusRepository.updateEnhancement(record);

        // 3. Determine Inputs
        EnhancementRecord lastFinished = findPreviousEnhancement(parentGeneration, record.getIndex());

        // 4. Build & Schedule Event
        EnhancementCreated retryEvent = sbomMapper.toEnhancementCreatedEvent(record, lastFinished, parentGeneration);
        enhancementScheduler.schedule(retryEvent);
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
