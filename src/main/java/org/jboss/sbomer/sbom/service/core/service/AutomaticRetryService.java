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
 * This service evaluates failed operations against configured retry policies and triggers
 * immediate retries when appropriate. Retry decisions are based on:
 * - Global retry enablement flag
 * - Error type retryability configuration
 * - Current attempt count vs. maximum allowed attempts
 *
 * Retries are transparent to external generators/enhancers - they receive normal
 * generation/enhancement requests with no indication that this is a retry attempt.
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
     * 2. The error type is configured as retryable (based on canonical error code)
     * 3. The current attempt count is below the configured maximum
     *
     * @param generationId the ID of the failed generation
     * @param failureResult the failure reason/error code (legacy)
     * @return true if retry was triggered, false otherwise
     */
    public boolean tryRetryGeneration(String generationId, GenerationResult failureResult) {
        if (!config.isRetryEnabled()) {
            log.debug("Retry disabled globally, skipping retry for generation {}", generationId);
            return false;
        }

        // Map legacy result to canonical error code
        Optional<ErrorResult> canonicalError = ErrorMapper.fromGenerationResult(failureResult);

        // Use canonical error code retryability
        if (canonicalError.isEmpty() || !canonicalError.get().isRetryable()) {
            log.debug(
                    "Error {} (canonical: {}) is not retryable, skipping retry for generation {}",
                    failureResult,
                    canonicalError.orElse(null),
                    generationId);
            return false;
        }

        // Count existing attempts
        List<GenerationRunRecord> runs = statusRepository.findGenerationRunsByGenerationId(generationId);
        int totalAttempts = runs.size();
        int maxAttempts = config.getMaxAttemptsForGeneration(failureResult);

        if (totalAttempts >= maxAttempts) {
            log.info(
                    "Max retry attempts reached for generation {}: {} >= {}, error={}",
                    generationId,
                    totalAttempts,
                    maxAttempts,
                    failureResult);
            return false;
        }

        // Trigger immediate retry
        log.info(
                "Triggering immediate retry for generation {}: attempt {}/{}, legacyError={}, canonicalError={}",
                generationId,
                totalAttempts + 1,
                maxAttempts,
                failureResult,
                canonicalError);

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
     * 2. The error type is configured as retryable (based on canonical error code)
     * 3. The current attempt count is below the configured maximum
     *
     * @param enhancementId the ID of the failed enhancement
     * @param failureResult the failure reason/error code (legacy)
     * @return true if retry was triggered, false otherwise
     */
    public boolean tryRetryEnhancement(String enhancementId, EnhancementResult failureResult) {
        if (!config.isRetryEnabled()) {
            log.debug("Retry disabled globally, skipping retry for enhancement {}", enhancementId);
            return false;
        }

        // Map legacy result to canonical error code
        Optional<ErrorResult> canonicalError = ErrorMapper.fromEnhancementResult(failureResult);

        // Use canonical error code retryability
        if (canonicalError.isEmpty() || !canonicalError.get().isRetryable()) {
            log.debug(
                    "Error {} (canonical: {}) is not retryable, skipping retry for enhancement {}",
                    failureResult,
                    canonicalError.orElse(null),
                    enhancementId);
            return false;
        }

        // Count existing attempts
        List<EnhancementRunRecord> runs = statusRepository.findEnhancementRunsByEnhancementId(enhancementId);
        int totalAttempts = runs.size();
        int maxAttempts = config.getMaxAttemptsForEnhancement(failureResult);

        if (totalAttempts >= maxAttempts) {
            log.info(
                    "Max retry attempts reached for enhancement {}: {} >= {}, error={}",
                    enhancementId,
                    totalAttempts,
                    maxAttempts,
                    failureResult);
            return false;
        }

        // Trigger immediate retry
        log.info(
                "Triggering immediate retry for enhancement {}: attempt {}/{}, legacyError={}, canonicalError={}",
                enhancementId,
                totalAttempts + 1,
                maxAttempts,
                failureResult,
                canonicalError);

        try {
            // Create new run and update status to PENDING_RETRY
            runManagement.retryEnhancement(enhancementId);

            // Fetch the updated enhancement record
            EnhancementRecord record = statusRepository.findEnhancementById(enhancementId);

            // Fetch parent generation to get SBOM location
            GenerationRecord parentGeneration = statusRepository.findGenerationById(record.getGenerationId());

            // Determine inputs (previous enhancement in chain)
            EnhancementRecord lastFinished = findPreviousEnhancement(parentGeneration, record.getIndex());

            // Build & Schedule Event
            EnhancementCreated retryEvent = sbomMapper.toEnhancementCreatedEvent(record, lastFinished, parentGeneration);
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

