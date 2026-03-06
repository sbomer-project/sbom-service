package org.jboss.sbomer.sbom.service.core.service;

import java.time.Instant;
import java.util.*;

import org.jboss.sbomer.events.common.GenerationRequestSpec;
import org.jboss.sbomer.events.enhancer.EnhancementUpdate;
import org.jboss.sbomer.events.generator.GenerationUpdate;
import org.jboss.sbomer.events.orchestration.*;
import org.jboss.sbomer.events.request.RequestsCreated;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.RequestStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.RunState;
import org.jboss.sbomer.sbom.service.core.port.api.RunManagement;
import org.jboss.sbomer.sbom.service.core.port.api.enhancement.EnhancementStatusProcessor;
import org.jboss.sbomer.sbom.service.core.port.api.generation.GenerationProcessor;
import org.jboss.sbomer.sbom.service.core.port.api.generation.GenerationStatusProcessor;
import org.jboss.sbomer.sbom.service.core.port.spi.FailureNotifier;
import org.jboss.sbomer.sbom.service.core.port.spi.RecipeBuilder;
import org.jboss.sbomer.sbom.service.core.port.spi.RequestsFinishedNotifier;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;
import org.jboss.sbomer.sbom.service.core.port.spi.enhancement.EnhancementScheduler;
import org.jboss.sbomer.sbom.service.core.port.spi.generation.GenerationScheduler;
import org.jboss.sbomer.sbom.service.core.utility.TsidUtility;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class SbomService implements GenerationProcessor, GenerationStatusProcessor, EnhancementStatusProcessor {

    GenerationScheduler generationScheduler;
    EnhancementScheduler enhancementScheduler;
    SbomMapper sbomMapper;
    StatusRepository statusRepository;
    RecipeBuilder recipeBuilder;
    RequestsFinishedNotifier requestsFinishedNotifier;
    FailureNotifier failureNotifier;
    RunManagement runManagement;

    @Inject
    public SbomService(GenerationScheduler generationScheduler, EnhancementScheduler enhancementScheduler, SbomMapper sbomMapper, StatusRepository statusRepository, RecipeBuilder recipeBuilder, RequestsFinishedNotifier requestsFinishedNotifier, FailureNotifier failureNotifier, RunManagement runManagement) {
        this.generationScheduler = generationScheduler;
        this.enhancementScheduler = enhancementScheduler;
        this.sbomMapper = sbomMapper;
        this.statusRepository = statusRepository;
        this.recipeBuilder = recipeBuilder;
        this.requestsFinishedNotifier = requestsFinishedNotifier;
        this.failureNotifier = failureNotifier;
        this.runManagement = runManagement;
    }

    // Create recipes for each generation requested from the source and schedule them to be generated
    @WithSpan
    @Override
    public void processGenerations(RequestsCreated requestsCreatedEvent) {
        // Get list of generation requests
        List<GenerationRequestSpec> generationRequestSpecs = requestsCreatedEvent.getData().getGenerationRequests();

        // First create a RequestRecord to track all the generations
        RequestRecord requestRecord = sbomMapper.toNewRequestRecord(requestsCreatedEvent);
        statusRepository.saveRequestRecord(requestRecord);

        // For each generation request specification, prepare and fire off a "generation.created" event
        for (GenerationRequestSpec generationRequestSpec : generationRequestSpecs) {
            // Create a generation record for tracking and save to data source
            GenerationRecord generationRecord = sbomMapper.toNewGenerationRecord(generationRequestSpec, requestsCreatedEvent.getData().getRequestId());
            statusRepository.saveGeneration(generationRecord);
            
            // Create the initial Run entity (Attempt #1) for this generation
            GenerationRunRecord initialRun = new GenerationRunRecord();
            initialRun.setId(TsidUtility.createUniqueRunId());
            initialRun.setGenerationId(generationRecord.getId());
            initialRun.setAttemptNumber(1);
            initialRun.setState(RunState.PENDING);
            initialRun.setMessage("Initial generation attempt");
            initialRun.setStartTime(Instant.now());
            statusRepository.saveGenerationRun(initialRun);
            
            log.info("Created initial GenerationRun: runId={}, generationId={}, attempt=1",
                    initialRun.getId(), generationRecord.getId());
            
            // Schedule the new generation (i.e. send generation.created event to the system)
            GenerationCreated generationCreatedEvent = sbomMapper.toGenerationCreatedEvent(generationRecord, generationRequestSpec, requestsCreatedEvent.getData().getRequestId());
            generationScheduler.schedule(generationCreatedEvent);
        }

    }

    // Process the incoming updates from the generators
    @WithSpan
    @Override
    public void processGenerationStatusUpdate(GenerationUpdate generationUpdate) {
        String generationId = generationUpdate.getData().getGenerationId();

        GenerationRecord record = statusRepository.findGenerationById(generationId);
        if (record == null) {
            log.warn("Received update for unknown Generation ID: {}. Ignoring.", generationId);
            return;
        }

        switch (generationUpdate.getData().getStatus()) {
            case "GENERATING":
                // Find or create active run and update its state to RUNNING
                String runningRunId = findOrCreateActiveGenerationRun(generationId);
                GenerationRunRecord runningRun = statusRepository.findGenerationRunById(runningRunId);
                runningRun.setState(RunState.RUNNING);
                statusRepository.updateGenerationRun(runningRun);
                
                // Update generation status to GENERATING
                GenerationRecord inProgressGenerationRecord = statusRepository.findGenerationById(generationId);
                inProgressGenerationRecord.setStatus(GenerationStatus.GENERATING);
                inProgressGenerationRecord.setUpdated(Instant.now());
                statusRepository.updateGeneration(inProgressGenerationRecord);
                break;

            case "FINISHED":
                // Find or create active run and complete it
                String finishedRunId = findOrCreateActiveGenerationRun(generationId);
                GenerationResult successResult = GenerationResult.fromCode(generationUpdate.getData().getResultCode())
                        .orElse(GenerationResult.ERR_GENERAL);
                runManagement.completeGenerationRun(finishedRunId, successResult, "Generation completed");
                
                // Update SBOM URLs (still needed as this is domain-specific data)
                GenerationRecord finishedGenerationRecord = statusRepository.findGenerationById(generationId);
                finishedGenerationRecord.setGenerationSbomUrls(generationUpdate.getData().getBaseSbomUrls());
                statusRepository.updateGeneration(finishedGenerationRecord);
                
                triggerNextStepForGeneration(finishedGenerationRecord.getId(), finishedGenerationRecord.getRequestId());
                break;

            case "FAILED":
                // Find or create active run and complete it with failure
                String failedRunId = findOrCreateActiveGenerationRun(generationId);
                GenerationResult failureResult = GenerationResult.fromCode(generationUpdate.getData().getResultCode())
                        .orElse(GenerationResult.ERR_GENERAL);
                String failureReason = generationUpdate.getData().getReason();
                runManagement.completeGenerationRun(failedRunId, failureResult, failureReason);
                break;
        }
    }

    @WithSpan
    @Override
    public void processEnhancementStatusUpdate(EnhancementUpdate enhancementUpdate) {
        String enhancementId = enhancementUpdate.getData().getEnhancementId();

        EnhancementRecord record = statusRepository.findEnhancementById(enhancementId);
        if (record == null) {
            log.warn("Received update for unknown Enhancement ID: {}. Ignoring.", enhancementId);
            return;
        }

        switch (enhancementUpdate.getData().getStatus()) {
            case "ENHANCING":
                // Find or create active run and update its state to RUNNING
                String runningRunId = findOrCreateActiveEnhancementRun(enhancementId);
                EnhancementRunRecord runningRun = statusRepository.findEnhancementRunById(runningRunId);
                runningRun.setState(RunState.RUNNING);
                statusRepository.updateEnhancementRun(runningRun);
                
                // Update enhancement status to ENHANCING
                EnhancementRecord inProgressEnhancementRecord = statusRepository.findEnhancementById(enhancementId);
                inProgressEnhancementRecord.setStatus(EnhancementStatus.ENHANCING);
                inProgressEnhancementRecord.setUpdated(Instant.now());
                statusRepository.updateEnhancement(inProgressEnhancementRecord);
                break;

            case "FINISHED":
                // Find or create active run and complete it
                String finishedRunId = findOrCreateActiveEnhancementRun(enhancementId);
                EnhancementResult successResult = EnhancementResult.fromCode(enhancementUpdate.getData().getResultCode())
                        .orElse(EnhancementResult.ERR_GENERAL);
                runManagement.completeEnhancementRun(finishedRunId, successResult, "Enhancement completed");
                
                // Update SBOM URLs (still needed as this is domain-specific data)
                EnhancementRecord finishedEnhancementRecord = statusRepository.findEnhancementById(enhancementId);
                finishedEnhancementRecord.setEnhancedSbomUrls(enhancementUpdate.getData().getEnhancedSbomUrls());
                statusRepository.updateEnhancement(finishedEnhancementRecord);
                
                // Important step to continue the process for the generation
                triggerNextStepForGeneration(finishedEnhancementRecord.getGenerationId(), finishedEnhancementRecord.getRequestId());
                break;

            case "FAILED":
                // Find or create active run and complete it with failure
                String failedRunId = findOrCreateActiveEnhancementRun(enhancementId);
                EnhancementResult failureResult = EnhancementResult.fromCode(enhancementUpdate.getData().getResultCode())
                        .orElse(EnhancementResult.ERR_GENERAL);
                String failureReason = enhancementUpdate.getData().getReason();
                runManagement.completeEnhancementRun(failedRunId, failureResult, failureReason);
                break;
        }
    }

    private void triggerNextStepForGeneration(String generationId, String requestId) {

        if (statusRepository.isGenerationAndEnhancementsFinished(generationId)) {
            if (statusRepository.isAllGenerationRequestsFinished(requestId)) {
                // ALL Generations and Enhancements finished
                RequestRecord requestRecord = statusRepository.findRequestById(requestId);
                requestRecord.setStatus(RequestStatus.COMPLETED);

                // Update request status to FINISHED
                statusRepository.updateRequestRecord(requestRecord);

                RequestsFinished requestsFinishedEvent = sbomMapper.toRequestsFinishedEvent(requestRecord);

                requestsFinishedNotifier.notify(requestsFinishedEvent);
                // We have notified that all the generations for a given request have been finished.
                return;
            }
            // Generation and enhancements for the specific generation are complete,
            // but not the whole request. No need to do anything
            return;
        }
        triggerNextEnhancement(generationId);
    }

    private void triggerNextEnhancement(String generationId) {
        GenerationRecord generationRecord = statusRepository.findGenerationById(generationId);

        // We first get the enhancement records properly sorted by the index to preserve order
        List<EnhancementRecord> sortedEnhancementRecords = generationRecord.getEnhancements().stream()
                .sorted(Comparator.comparingInt(EnhancementRecord::getIndex))
                .toList();

        EnhancementRecord lastFinished = null;

        for (EnhancementRecord current : sortedEnhancementRecords) {
            // We find the last finished enhancement
            if (EnhancementStatus.COMPLETED.equals(current.getStatus())) {
                lastFinished = current;
                continue;
            }

            // We find an enhancement with status NEW
            if (EnhancementStatus.NEW.equals(current.getStatus())) {
                // Create the initial Run entity (Attempt #1) for this enhancement
                EnhancementRunRecord initialRun = new EnhancementRunRecord();
                initialRun.setId(TsidUtility.createUniqueRunId());
                initialRun.setEnhancementId(current.getId());
                initialRun.setAttemptNumber(1);
                initialRun.setState(RunState.PENDING);
                initialRun.setMessage("Initial enhancement attempt");
                initialRun.setStartTime(Instant.now());
                statusRepository.saveEnhancementRun(initialRun);
                
                log.info("Created initial EnhancementRun: runId={}, enhancementId={}, attempt=1",
                        initialRun.getId(), current.getId());
                
                // lastFinished might be null if the very first record is NEW (expected)
                enhancementScheduler.schedule(sbomMapper.toEnhancementCreatedEvent(current, lastFinished, generationRecord));
                return;
            }

            // TODO Chain is not consistent, handle here (e.g. Did not follow FINISHED-FINISHED-NEW)
            return;
        }
    }

    // ==================== HELPER METHODS FOR RUN MANAGEMENT ====================

    /**
     * Find or create an active GenerationRun for the given Generation.
     * This implements Option 2 from the integration plan: infer Run from domain entity.
     *
     * @param generationId The ID of the generation
     * @return The ID of the active run (PENDING or RUNNING)
     */
    private String findOrCreateActiveGenerationRun(String generationId) {
        List<GenerationRunRecord> runs = statusRepository.findGenerationRunsByGenerationId(generationId);
        
        // Find active run (PENDING or RUNNING)
        Optional<GenerationRunRecord> activeRun = runs.stream()
                .filter(r -> r.getState() == RunState.PENDING || r.getState() == RunState.RUNNING)
                .findFirst();
            
        if (activeRun.isPresent()) {
            log.debug("Found existing active GenerationRun: runId={}, state={}",
                    activeRun.get().getId(), activeRun.get().getState());
            return activeRun.get().getId();
        }
        
        // No active run found, create new one
        int nextAttempt = runs.stream()
                .mapToInt(GenerationRunRecord::getAttemptNumber)
                .max()
                .orElse(0) + 1;
        
        GenerationRunRecord newRun = new GenerationRunRecord();
        newRun.setId(TsidUtility.createUniqueRunId());
        newRun.setGenerationId(generationId);
        newRun.setAttemptNumber(nextAttempt);
        newRun.setState(RunState.PENDING);
        newRun.setMessage("Automatically created run for attempt " + nextAttempt);
        newRun.setStartTime(Instant.now());
        statusRepository.saveGenerationRun(newRun);
        
        log.info("Created new GenerationRun: runId={}, generationId={}, attempt={}",
                newRun.getId(), generationId, nextAttempt);
        
        return newRun.getId();
    }

    /**
     * Find or create an active EnhancementRun for the given Enhancement.
     * This implements Option 2 from the integration plan: infer Run from domain entity.
     *
     * @param enhancementId The ID of the enhancement
     * @return The ID of the active run (PENDING or RUNNING)
     */
    private String findOrCreateActiveEnhancementRun(String enhancementId) {
        List<EnhancementRunRecord> runs = statusRepository.findEnhancementRunsByEnhancementId(enhancementId);
        
        // Find active run (PENDING or RUNNING)
        Optional<EnhancementRunRecord> activeRun = runs.stream()
                .filter(r -> r.getState() == RunState.PENDING || r.getState() == RunState.RUNNING)
                .findFirst();
            
        if (activeRun.isPresent()) {
            log.debug("Found existing active EnhancementRun: runId={}, state={}",
                    activeRun.get().getId(), activeRun.get().getState());
            return activeRun.get().getId();
        }
        
        // No active run found, create new one
        int nextAttempt = runs.stream()
                .mapToInt(EnhancementRunRecord::getAttemptNumber)
                .max()
                .orElse(0) + 1;
        
        EnhancementRunRecord newRun = new EnhancementRunRecord();
        newRun.setId(TsidUtility.createUniqueRunId());
        newRun.setEnhancementId(enhancementId);
        newRun.setAttemptNumber(nextAttempt);
        newRun.setState(RunState.PENDING);
        newRun.setMessage("Automatically created run for attempt " + nextAttempt);
        newRun.setStartTime(Instant.now());
        statusRepository.saveEnhancementRun(newRun);
        
        log.info("Created new EnhancementRun: runId={}, enhancementId={}, attempt={}",
                newRun.getId(), enhancementId, nextAttempt);
        
        return newRun.getId();
    }

}
