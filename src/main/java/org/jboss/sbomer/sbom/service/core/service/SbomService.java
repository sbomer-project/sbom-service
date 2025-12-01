package org.jboss.sbomer.sbom.service.core.service;

import java.time.Instant;
import java.util.*;

import org.jboss.sbomer.events.common.GenerationRequestSpec;
import org.jboss.sbomer.events.enhancer.EnhancementUpdate;
import org.jboss.sbomer.events.generator.GenerationUpdate;
import org.jboss.sbomer.events.orchestration.*;
import org.jboss.sbomer.events.request.RequestsCreated;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;
import org.jboss.sbomer.sbom.service.core.port.api.enhancement.EnhancementStatusProcessor;
import org.jboss.sbomer.sbom.service.core.port.api.generation.GenerationProcessor;
import org.jboss.sbomer.sbom.service.core.port.api.generation.GenerationStatusProcessor;
import org.jboss.sbomer.sbom.service.core.port.spi.FailureNotifier;
import org.jboss.sbomer.sbom.service.core.port.spi.RecipeBuilder;
import org.jboss.sbomer.sbom.service.core.port.spi.RequestsFinishedNotifier;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;
import org.jboss.sbomer.sbom.service.core.port.spi.enhancement.EnhancementScheduler;
import org.jboss.sbomer.sbom.service.core.port.spi.generation.GenerationScheduler;

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

    @Inject
    public SbomService(GenerationScheduler generationScheduler, EnhancementScheduler enhancementScheduler, SbomMapper sbomMapper, StatusRepository statusRepository, RecipeBuilder recipeBuilder, RequestsFinishedNotifier requestsFinishedNotifier, FailureNotifier failureNotifier) {
        this.generationScheduler = generationScheduler;
        this.enhancementScheduler = enhancementScheduler;
        this.sbomMapper = sbomMapper;
        this.statusRepository = statusRepository;
        this.recipeBuilder = recipeBuilder;
        this.requestsFinishedNotifier = requestsFinishedNotifier;
        this.failureNotifier = failureNotifier;
    }

    // Create recipes for each generation requested from the source and schedule them to be generated
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
            // Schedule the new generation (i.e. send generation.created event to the system)
            GenerationCreated generationCreatedEvent = sbomMapper.toGenerationCreatedEvent(generationRecord, generationRequestSpec, requestsCreatedEvent.getData().getRequestId());
            generationScheduler.schedule(generationCreatedEvent);
        }

    }

    // Process the incoming updates from the generators
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
                // update generation status to GENERATING
                GenerationRecord inProgressGenerationRecord = statusRepository.findGenerationById(generationId);
                inProgressGenerationRecord.setStatus(GenerationStatus.GENERATING);
                inProgressGenerationRecord.setUpdated(Instant.now());
                statusRepository.updateGeneration(inProgressGenerationRecord);
                break;

            case "FINISHED":
                // update generation status to FINISHED
                GenerationRecord finishedGenerationRecord = statusRepository.findGenerationById(generationId);
                finishedGenerationRecord.setStatus(GenerationStatus.FINISHED);
                finishedGenerationRecord.setResult(generationUpdate.getData().getResultCode());
                finishedGenerationRecord.setUpdated(Instant.now());
                finishedGenerationRecord.setFinished(Instant.now());
                //  IMPORTANT part is to get the SBOM urls from the FINISHED update
                finishedGenerationRecord.setGenerationSbomUrls(generationUpdate.getData().getBaseSbomUrls());
                statusRepository.updateGeneration(finishedGenerationRecord);
                triggerNextStepForGeneration(finishedGenerationRecord.getId(), finishedGenerationRecord.getRequestId());
                break;

            case "FAILED":
                // update generation status to FAILED
                GenerationRecord failedGenerationRecord = statusRepository.findGenerationById(generationId);
                failedGenerationRecord.setStatus(GenerationStatus.FAILED);
                failedGenerationRecord.setResult(generationUpdate.getData().getResultCode());
                failedGenerationRecord.setReason(generationUpdate.getData().getReason());
                failedGenerationRecord.setUpdated(Instant.now());
                failedGenerationRecord.setFinished(Instant.now());
                statusRepository.updateGeneration(failedGenerationRecord);
                break;
        }
    }

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
                // update enhancement status to ENHANCING
                EnhancementRecord inProgressEnhancementRecord = statusRepository.findEnhancementById(enhancementId);
                inProgressEnhancementRecord.setStatus(EnhancementStatus.ENHANCING);
                inProgressEnhancementRecord.setUpdated(Instant.now());
                statusRepository.updateEnhancement(inProgressEnhancementRecord);
                break;

            case "FINISHED":
                // update enhancement status to FINISHED
                EnhancementRecord finishedEnhancementRecord = statusRepository.findEnhancementById(enhancementId);
                finishedEnhancementRecord.setStatus(EnhancementStatus.FINISHED);
                finishedEnhancementRecord.setResult(enhancementUpdate.getData().getResultCode());
                finishedEnhancementRecord.setUpdated(Instant.now());
                finishedEnhancementRecord.setFinished(Instant.now());
                //  IMPORTANT part is to get the SBOM urls from the FINISHED update
                finishedEnhancementRecord.setEnhancedSbomUrls(enhancementUpdate.getData().getEnhancedSbomUrls());
                statusRepository.updateEnhancement(finishedEnhancementRecord);
                // Important step to continue the process for the generation
                triggerNextStepForGeneration(finishedEnhancementRecord.getGenerationId(), finishedEnhancementRecord.getRequestId());
                break;

            case "FAILED":
                // update enhancement status to FAILED
                EnhancementRecord failedEnhancementRecord = statusRepository.findEnhancementById(enhancementId);
                failedEnhancementRecord.setStatus(EnhancementStatus.FAILED);
                failedEnhancementRecord.setResult(enhancementUpdate.getData().getResultCode());
                failedEnhancementRecord.setReason(enhancementUpdate.getData().getReason());
                failedEnhancementRecord.setUpdated(Instant.now());
                failedEnhancementRecord.setFinished(Instant.now());
                statusRepository.updateEnhancement(failedEnhancementRecord);
                break;
        }
    }

    private void triggerNextStepForGeneration(String generationId, String requestId) {

        if (statusRepository.isGenerationAndEnhancementsFinished(generationId)) {
            if (statusRepository.isAllGenerationRequestsFinished(requestId)) {
                RequestRecord requestRecord = statusRepository.findRequestById(requestId);
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
            if (EnhancementStatus.FINISHED.equals(current.getStatus())) {
                lastFinished = current;
                continue;
            }

            // We find an enhancement with status NEW
            if (EnhancementStatus.NEW.equals(current.getStatus())) {
                // lastFinished might be null if the very first record is NEW (expected)
                enhancementScheduler.schedule(sbomMapper.toEnhancementCreatedEvent(current, lastFinished, generationRecord));
                return;
            }

            // TODO Chain is not consistent, handle here (e.g. Did not follow FINISHED-FINISHED-NEW)
            return;
        }
    }

}
