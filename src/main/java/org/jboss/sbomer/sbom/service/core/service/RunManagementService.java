package org.jboss.sbomer.sbom.service.core.service;

import java.time.Instant;
import java.util.List;

import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.ChildEnhancementsStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.ChildGenerationsStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.RequestStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.RunState;
import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;
import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;
import org.jboss.sbomer.sbom.service.core.port.api.RunManagement;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;
import org.jboss.sbomer.sbom.service.core.utility.TsidUtility;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementing the Run Management business logic for execution tracking and manual retries.
 *
 * This service handles:
 * 1. Bottom-up roll-up: When a Run completes, update parent domain entity and propagate status up the tree
 * 2. Top-down resurrection: When retrying a failed entity, create new Run and update parent statuses
 */
@ApplicationScoped
@Transactional
@Slf4j
public class RunManagementService implements RunManagement {

    @Inject
    StatusRepository repository;

    // ==================== GENERATION RUN COMPLETION (Bottom-Up Roll-up) ====================

    @Override
    public void completeGenerationRun(String runId, GenerationResult result, String message) {
        log.info("Completing GenerationRun: runId={}, result={}", runId, result);

        // 1. Fetch and update the Run
        GenerationRunRecord run = repository.findGenerationRunById(runId);
        if (run == null) {
            throw new EntityNotFoundException("GenerationRun not found: " + runId);
        }

        RunState finalState = (result == GenerationResult.SUCCESS) ? RunState.SUCCEEDED : RunState.FAILED;
        run.setState(finalState);
        run.setReason(result);
        run.setMessage(message);
        run.setCompletionTime(Instant.now());
        repository.updateGenerationRun(run);

        // 2. Update parent Generation
        GenerationRecord generation = repository.findGenerationById(run.getGenerationId());
        if (generation == null) {
            throw new EntityNotFoundException("Generation not found: " + run.getGenerationId());
        }

        GenerationStatus newGenerationStatus = (finalState == RunState.SUCCEEDED)
                ? GenerationStatus.COMPLETED
                : GenerationStatus.FAILED;
        generation.setStatus(newGenerationStatus);
        generation.setLatestResult(result);
        generation.setUpdated(Instant.now());
        if (finalState == RunState.SUCCEEDED || finalState == RunState.FAILED) {
            generation.setFinished(Instant.now());
        }
        repository.updateGeneration(generation);

        log.info("Updated Generation: id={}, status={}, latestResult={}",
                generation.getId(), newGenerationStatus, result);

        // 3. Roll up to Request (if Generation has a parent Request)
        if (generation.getRequestId() != null) {
            rollUpGenerationToRequest(generation.getRequestId());
        }
    }

    @Override
    public void completeEnhancementRun(String runId, EnhancementResult result, String message) {
        log.info("Completing EnhancementRun: runId={}, result={}", runId, result);

        // 1. Fetch and update the Run
        EnhancementRunRecord run = repository.findEnhancementRunById(runId);
        if (run == null) {
            throw new EntityNotFoundException("EnhancementRun not found: " + runId);
        }

        RunState finalState = (result == EnhancementResult.SUCCESS) ? RunState.SUCCEEDED : RunState.FAILED;
        run.setState(finalState);
        run.setReason(result);
        run.setMessage(message);
        run.setCompletionTime(Instant.now());
        repository.updateEnhancementRun(run);

        // 2. Update parent Enhancement
        EnhancementRecord enhancement = repository.findEnhancementById(run.getEnhancementId());
        if (enhancement == null) {
            throw new EntityNotFoundException("Enhancement not found: " + run.getEnhancementId());
        }

        EnhancementStatus newEnhancementStatus = (finalState == RunState.SUCCEEDED)
                ? EnhancementStatus.COMPLETED
                : EnhancementStatus.FAILED;
        enhancement.setStatus(newEnhancementStatus);
        enhancement.setLatestResult(result);
        enhancement.setUpdated(Instant.now());
        if (finalState == RunState.SUCCEEDED || finalState == RunState.FAILED) {
            enhancement.setFinished(Instant.now());
        }
        repository.updateEnhancement(enhancement);

        log.info("Updated Enhancement: id={}, status={}, latestResult={}",
                enhancement.getId(), newEnhancementStatus, result);

        // 3. Roll up to Generation (if Enhancement has a parent Generation)
        if (enhancement.getGenerationId() != null) {
            rollUpEnhancementToGeneration(enhancement.getGenerationId());
        }
    }

    // ==================== MANUAL RETRY (Top-Down Resurrection) ====================

    @Override
    public GenerationRunRecord retryGeneration(String generationId) {
        log.info("Retrying Generation: id={}", generationId);

        // 1. Fetch the Generation
        GenerationRecord generation = repository.findGenerationById(generationId);
        if (generation == null) {
            throw new EntityNotFoundException("Generation not found: " + generationId);
        }

        // 2. Validate that it's in a retryable state
        if (generation.getStatus() != GenerationStatus.FAILED) {
            throw new InvalidRetryStateException(
                    "Generation must be in FAILED state to retry. Current state: " + generation.getStatus());
        }

        // 3. Get the next attempt number
        List<GenerationRunRecord> existingRuns = repository.findGenerationRunsByGenerationId(generationId);
        int nextAttempt = existingRuns.stream()
                .mapToInt(GenerationRunRecord::getAttemptNumber)
                .max()
                .orElse(0) + 1;

        // 4. Create a new Run (Attempt N+1)
        GenerationRunRecord newRun = new GenerationRunRecord();
        newRun.setId(TsidUtility.createUniqueRunId());
        newRun.setGenerationId(generationId);
        newRun.setAttemptNumber(nextAttempt);
        newRun.setState(RunState.PENDING);
        newRun.setReason(null); // Will be set when run completes
        newRun.setMessage("Manual retry attempt " + nextAttempt);
        newRun.setStartTime(Instant.now());
        newRun.setCompletionTime(null);
        repository.saveGenerationRun(newRun);

        log.info("Created new GenerationRun: runId={}, attempt={}", newRun.getId(), nextAttempt);

        // 5. Resurrect the Generation (FAILED -> GENERATING)
        generation.setStatus(GenerationStatus.GENERATING);
        generation.setLatestResult(null); // Clear the failure reason
        generation.setUpdated(Instant.now());
        generation.setFinished(null); // No longer finished
        repository.updateGeneration(generation);

        log.info("Resurrected Generation: id={}, status={}", generationId, GenerationStatus.GENERATING);

        // 6. Reverse roll-up to Request (if applicable)
        if (generation.getRequestId() != null) {
            reverseRollUpGenerationToRequest(generation.getRequestId());
        }

        return newRun;
    }

    @Override
    public EnhancementRunRecord retryEnhancement(String enhancementId) {
        log.info("Retrying Enhancement: id={}", enhancementId);

        // 1. Fetch the Enhancement
        EnhancementRecord enhancement = repository.findEnhancementById(enhancementId);
        if (enhancement == null) {
            throw new EntityNotFoundException("Enhancement not found: " + enhancementId);
        }

        // 2. Validate that it's in a retryable state
        if (enhancement.getStatus() != EnhancementStatus.FAILED) {
            throw new InvalidRetryStateException(
                    "Enhancement must be in FAILED state to retry. Current state: " + enhancement.getStatus());
        }

        // 3. Get the next attempt number
        List<EnhancementRunRecord> existingRuns = repository.findEnhancementRunsByEnhancementId(enhancementId);
        int nextAttempt = existingRuns.stream()
                .mapToInt(EnhancementRunRecord::getAttemptNumber)
                .max()
                .orElse(0) + 1;

        // 4. Create a new Run (Attempt N+1)
        EnhancementRunRecord newRun = new EnhancementRunRecord();
        newRun.setId(TsidUtility.createUniqueRunId());
        newRun.setEnhancementId(enhancementId);
        newRun.setAttemptNumber(nextAttempt);
        newRun.setState(RunState.PENDING);
        newRun.setReason(null); // Will be set when run completes
        newRun.setMessage("Manual retry attempt " + nextAttempt);
        newRun.setStartTime(Instant.now());
        newRun.setCompletionTime(null);
        repository.saveEnhancementRun(newRun);

        log.info("Created new EnhancementRun: runId={}, attempt={}", newRun.getId(), nextAttempt);

        // 5. Resurrect the Enhancement (FAILED -> ENHANCING)
        enhancement.setStatus(EnhancementStatus.ENHANCING);
        enhancement.setLatestResult(null); // Clear the failure reason
        enhancement.setUpdated(Instant.now());
        enhancement.setFinished(null); // No longer finished
        repository.updateEnhancement(enhancement);

        log.info("Resurrected Enhancement: id={}, status={}", enhancementId, EnhancementStatus.ENHANCING);

        // 6. Reverse roll-up to Generation (if applicable)
        if (enhancement.getGenerationId() != null) {
            reverseRollUpEnhancementToGeneration(enhancement.getGenerationId());
        }

        return newRun;
    }

    // ==================== ROLL-UP HELPERS ====================

    /**
     * Roll up Generation statuses to the parent Request.
     * Recalculates childGenerationsStatus and overall RequestStatus.
     */
    private void rollUpGenerationToRequest(String requestId) {
        log.debug("Rolling up Generations to Request: requestId={}", requestId);

        RequestRecord request = repository.findRequestById(requestId);
        if (request == null) {
            log.warn("Request not found during roll-up: {}", requestId);
            return;
        }

        List<GenerationRecord> generations = repository.findGenerationsByRequestId(requestId);
        if (generations.isEmpty()) {
            log.warn("No generations found for Request: {}", requestId);
            return;
        }

        // Calculate aggregate child status
        ChildGenerationsStatus childStatus = calculateChildGenerationsStatus(generations);
        RequestStatus newRequestStatus = mapChildGenerationsStatusToRequestStatus(childStatus);

        request.setChildGenerationsStatus(childStatus);
        request.setStatus(newRequestStatus);
        repository.updateRequestRecord(request);

        log.info("Rolled up to Request: id={}, childGenerationsStatus={}, status={}",
                requestId, childStatus, newRequestStatus);
    }

    /**
     * Roll up Enhancement statuses to the parent Generation.
     * Recalculates childEnhancementsStatus.
     */
    private void rollUpEnhancementToGeneration(String generationId) {
        log.debug("Rolling up Enhancements to Generation: generationId={}", generationId);

        GenerationRecord generation = repository.findGenerationById(generationId);
        if (generation == null) {
            log.warn("Generation not found during roll-up: {}", generationId);
            return;
        }

        List<EnhancementRecord> enhancements = repository.findEnhancementsByGenerationId(generationId);
        if (enhancements.isEmpty()) {
            // No enhancements, set to NOT_APPLICABLE
            generation.setChildEnhancementsStatus(ChildEnhancementsStatus.NOT_APPLICABLE);
            repository.updateGeneration(generation);
            return;
        }

        // Calculate aggregate child status
        ChildEnhancementsStatus childStatus = calculateChildEnhancementsStatus(enhancements);
        generation.setChildEnhancementsStatus(childStatus);
        repository.updateGeneration(generation);

        log.info("Rolled up to Generation: id={}, childEnhancementsStatus={}", generationId, childStatus);

        // Continue roll-up to Request if applicable
        if (generation.getRequestId() != null) {
            rollUpGenerationToRequest(generation.getRequestId());
        }
    }

    /**
     * Reverse roll-up when a Generation is retried.
     * If Request was FAILED, move it back to PROCESSING.
     */
    private void reverseRollUpGenerationToRequest(String requestId) {
        log.debug("Reverse rolling up Generation retry to Request: requestId={}", requestId);

        RequestRecord request = repository.findRequestById(requestId);
        if (request == null) {
            log.warn("Request not found during reverse roll-up: {}", requestId);
            return;
        }

        // Only update if Request was in a terminal state
        if (request.getStatus() == RequestStatus.FAILED) {
            request.setStatus(RequestStatus.PROCESSING);
            request.setChildGenerationsStatus(ChildGenerationsStatus.PROCESSING);
            repository.updateRequestRecord(request);

            log.info("Reverse rolled up to Request: id={}, status={}", requestId, RequestStatus.PROCESSING);
        }
    }

    /**
     * Reverse roll-up when an Enhancement is retried.
     * If Generation's childEnhancementsStatus was FAILED, update it.
     */
    private void reverseRollUpEnhancementToGeneration(String generationId) {
        log.debug("Reverse rolling up Enhancement retry to Generation: generationId={}", generationId);

        GenerationRecord generation = repository.findGenerationById(generationId);
        if (generation == null) {
            log.warn("Generation not found during reverse roll-up: {}", generationId);
            return;
        }

        // Only update if childEnhancementsStatus was in a terminal state
        if (generation.getChildEnhancementsStatus() == ChildEnhancementsStatus.FAILED) {
            generation.setChildEnhancementsStatus(ChildEnhancementsStatus.PROCESSING);
            repository.updateGeneration(generation);

            log.info("Reverse rolled up to Generation: id={}, childEnhancementsStatus={}",
                    generationId, ChildEnhancementsStatus.PROCESSING);

            // Continue reverse roll-up to Request if applicable
            if (generation.getRequestId() != null) {
                reverseRollUpGenerationToRequest(generation.getRequestId());
            }
        }
    }

    // ==================== STATUS CALCULATION HELPERS ====================

    private ChildGenerationsStatus calculateChildGenerationsStatus(List<GenerationRecord> generations) {
        boolean anyProcessing = false;
        boolean anyFailed = false;
        boolean anySucceeded = false;

        for (GenerationRecord gen : generations) {
            switch (gen.getStatus()) {
                case NEW, GENERATING:
                    anyProcessing = true;
                    break;
                case FAILED:
                    anyFailed = true;
                    break;
                case COMPLETED:
                    anySucceeded = true;
                    break;
            }
        }

        if (anyProcessing) {
            return ChildGenerationsStatus.PROCESSING;
        } else if (anyFailed) {
            return ChildGenerationsStatus.FAILED;
        } else if (anySucceeded) {
            return ChildGenerationsStatus.COMPLETED;
        } else {
            return ChildGenerationsStatus.PENDING;
        }
    }

    private ChildEnhancementsStatus calculateChildEnhancementsStatus(List<EnhancementRecord> enhancements) {
        boolean anyProcessing = false;
        boolean anyFailed = false;
        boolean anySucceeded = false;

        for (EnhancementRecord enh : enhancements) {
            switch (enh.getStatus()) {
                case NEW, ENHANCING:
                    anyProcessing = true;
                    break;
                case FAILED:
                    anyFailed = true;
                    break;
                case COMPLETED:
                    anySucceeded = true;
                    break;
            }
        }

        if (anyProcessing) {
            return ChildEnhancementsStatus.PROCESSING;
        } else if (anyFailed) {
            return ChildEnhancementsStatus.FAILED;
        } else if (anySucceeded) {
            return ChildEnhancementsStatus.COMPLETED;
        } else {
            return ChildEnhancementsStatus.PENDING;
        }
    }

    private RequestStatus mapChildGenerationsStatusToRequestStatus(ChildGenerationsStatus childStatus) {
        return switch (childStatus) {
            case PROCESSING -> RequestStatus.PROCESSING;
            case COMPLETED -> RequestStatus.COMPLETED;
            case FAILED -> RequestStatus.FAILED;
            case PENDING -> RequestStatus.PENDING;
        };
    }
}
