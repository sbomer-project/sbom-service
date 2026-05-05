package org.jboss.sbomer.sbom.service.core.service;

import java.util.List;
import java.util.Optional;

import org.jboss.sbomer.events.common.GenerationRequestSpec;
import org.jboss.sbomer.events.orchestration.EnhancementCreated;
import org.jboss.sbomer.events.orchestration.GenerationCreated;
import org.jboss.sbomer.sbom.service.core.config.RetryPolicyConfig;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationResult;
import org.jboss.sbomer.sbom.service.core.port.api.RunManagement;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;
import org.jboss.sbomer.sbom.service.core.port.spi.enhancement.EnhancementScheduler;
import org.jboss.sbomer.sbom.service.core.port.spi.generation.GenerationScheduler;
import org.jboss.sbomer.sbom.service.core.utility.ErrorMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for automatic retry logic for failed generations and enhancements.
 *
 * This service evaluates failed operations and triggers immediate retries based on:
 * - Global retry enablement flag
 * - Canonical error code retryability (from ErrorResult enum)
 * - Configured maximum retry attempts per error type
 * - Current attempt count
 *
 * Retry decisions are made using canonical ErrorResult codes, which provide stable,
 * service-owned error classification independent of external worker implementations.
 */
@ApplicationScoped
@Slf4j
public class AutomaticRetryService {

    @Inject
    RetryPolicyConfig config;

    @Inject
    StatusRepository statusRepository;

    @Inject
    RunManagement runManagement;

    @Inject
    GenerationScheduler generationScheduler;

    @Inject
    EnhancementScheduler enhancementScheduler;

    @Inject
    SbomMapper sbomMapper;

    /**
     * Evaluates a failed generation and triggers an immediate retry if applicable.
     *
     * Retry is triggered only if:
     * 1. Retry is globally enabled
     * 2. The canonical error code is retryable (ErrorResult.isRetryable())
     * 3. Maximum retry attempts have not been exceeded
     *
     * @param generationId the ID of the failed generation
     * @param failureResult the legacy generation failure result (mapped to canonical error)
     * @return true if retry was triggered, false otherwise
     */
    public boolean tryRetryGeneration(String generationId, GenerationResult failureResult) {
        if (!config.isRetryEnabled()) {
            log.debug("Retry disabled globally, skipping retry for generation {}", generationId);
            return false;
        }

        // Map legacy result to canonical error code
        Optional<ErrorResult> canonicalError = ErrorMapper.fromGenerationResult(failureResult);

        // Check if error is retryable using canonical code
        if (canonicalError.isEmpty() || !canonicalError.get().isRetryable()) {
            log.debug(
                    "Error {} (canonical: {}) is not retryable, skipping retry for generation {}",
                    failureResult,
                    canonicalError.orElse(null),
                    generationId);
            return false;
        }

        ErrorResult error = canonicalError.get();

        // Count existing attempts
        List<GenerationRunRecord> runs = statusRepository.findGenerationRunsByGenerationId(generationId);
        int totalAttempts = runs.size();
        int maxAttempts = config.getMaxAttemptsForError(error);

        if (totalAttempts >= maxAttempts) {
            log.info(
                    "Max retry attempts reached for generation {}: {} >= {}, error={}",
                    generationId,
                    totalAttempts,
                    maxAttempts,
                    error);
            return false;
        }

        // Trigger immediate retry
        log.info(
                "Triggering immediate retry for generation {}: attempt {}/{}, error={}",
                generationId,
                totalAttempts + 1,
                maxAttempts,
                error);

        try {
            // Create new run and update status to PENDING_RETRY
            runManagement.retryGeneration(generationId);

            // Fetch the updated generation record
            GenerationRecord record = statusRepository.findGenerationById(generationId);

            // Reconstruct context and schedule the retry
            GenerationRequestSpec originalSpec = sbomMapper.toGenerationRequestSpec(record);
            String retryCorrelationId = record.getRequestId();

            // Build & Schedule Event
            GenerationCreated retryEvent = sbomMapper.toGenerationCreatedEvent(record, originalSpec, retryCorrelationId);
            generationScheduler.schedule(retryEvent);

            log.info("Successfully triggered retry for generation {}", generationId);
            return true;
        } catch (Exception e) {
            log.error("Failed to trigger retry for generation {}: {}", generationId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Evaluates a failed enhancement and triggers an immediate retry if applicable.
     *
     * Retry is triggered only if:
     * 1. Retry is globally enabled
     * 2. The canonical error code is retryable (ErrorResult.isRetryable())
     * 3. Maximum retry attempts have not been exceeded
     *
     * @param enhancementId the ID of the failed enhancement
     * @param failureResult the legacy enhancement failure result (mapped to canonical error)
     * @return true if retry was triggered, false otherwise
     */
    public boolean tryRetryEnhancement(String enhancementId, EnhancementResult failureResult) {
        if (!config.isRetryEnabled()) {
            log.debug("Retry disabled globally, skipping retry for enhancement {}", enhancementId);
            return false;
        }

        // Map legacy result to canonical error code
        Optional<ErrorResult> canonicalError = ErrorMapper.fromEnhancementResult(failureResult);

        // Check if error is retryable using canonical code
        if (canonicalError.isEmpty() || !canonicalError.get().isRetryable()) {
            log.debug(
                    "Error {} (canonical: {}) is not retryable, skipping retry for enhancement {}",
                    failureResult,
                    canonicalError.orElse(null),
                    enhancementId);
            return false;
        }

        ErrorResult error = canonicalError.get();

        // Count existing attempts
        List<EnhancementRunRecord> runs = statusRepository.findEnhancementRunsByEnhancementId(enhancementId);
        int totalAttempts = runs.size();
        int maxAttempts = config.getMaxAttemptsForError(error);

        if (totalAttempts >= maxAttempts) {
            log.info(
                    "Max retry attempts reached for enhancement {}: {} >= {}, error={}",
                    enhancementId,
                    totalAttempts,
                    maxAttempts,
                    error);
            return false;
        }

        // Trigger immediate retry
        log.info(
                "Triggering immediate retry for enhancement {}: attempt {}/{}, error={}",
                enhancementId,
                totalAttempts + 1,
                maxAttempts,
                error);

        try {
            // Create new run and update status to PENDING_RETRY
            runManagement.retryEnhancement(enhancementId);

            // Fetch the updated enhancement record
            EnhancementRecord currentEnhancement = statusRepository.findEnhancementById(enhancementId);

            // Fetch parent generation (required for enhancement event)
            GenerationRecord parentGeneration = statusRepository.findGenerationById(currentEnhancement.getGenerationId());

            // Find last finished enhancement (if any) - required for chaining
            EnhancementRecord lastFinished = findPreviousEnhancement(parentGeneration, currentEnhancement.getIndex());

            // Build & Schedule Event
            EnhancementCreated retryEvent = sbomMapper.toEnhancementCreatedEvent(
                currentEnhancement,
                lastFinished,
                parentGeneration
            );
            enhancementScheduler.schedule(retryEvent);

            log.info("Successfully triggered retry for enhancement {}", enhancementId);
            return true;
        } catch (Exception e) {
            log.error("Failed to trigger retry for enhancement {}: {}", enhancementId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Helper to find the enhancement that ran immediately before the target index.
     * Returns null if target is the first enhancement (index 0).
     */
    private EnhancementRecord findPreviousEnhancement(GenerationRecord parent, int targetIndex) {
        if (targetIndex == 0) {
            return null;
        }

        return parent.getEnhancements().stream()
                .filter(e -> e.getIndex() == targetIndex - 1)
                .findFirst()
                .orElse(null);
    }

}
