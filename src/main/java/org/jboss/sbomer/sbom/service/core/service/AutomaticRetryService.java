package org.jboss.sbomer.sbom.service.core.service;

import java.util.List;

import org.jboss.sbomer.events.common.GenerationRequestSpec;
import org.jboss.sbomer.events.orchestration.EnhancementCreated;
import org.jboss.sbomer.events.orchestration.GenerationCreated;
import org.jboss.sbomer.sbom.service.core.config.RetryPolicyConfig;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.port.api.RunManagement;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;
import org.jboss.sbomer.sbom.service.core.port.spi.enhancement.EnhancementScheduler;
import org.jboss.sbomer.sbom.service.core.port.spi.generation.GenerationScheduler;

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
 * Retry decisions are made using canonical {@link ErrorResult} codes, which provide stable,
 * service-owned error classification independent of external worker implementations.
 *
 * Retry flow:
 * <pre>
 * failure event received
 *   -> map legacy result to canonical ErrorResult
 *   -> verify retry is globally enabled
 *   -> verify canonical error is present and retryable
 *   -> count existing run attempts
 *   -> compare attempts against RetryPolicyConfig
 *   -> create a new run and mark entity PENDING_RETRY
 *   -> schedule a new generation/enhancement event
 * </pre>
 *
 * The retry is transparent to external workers. They receive a normal scheduling event and
 * do not need special retry-aware behavior.
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
     * 2. The canonical error code is retryable ({@link ErrorResult#isRetryable()})
     * 3. Maximum retry attempts have not been exceeded
     *
     * When eligible, the method creates a new generation run through {@link RunManagement},
     * reloads the generation aggregate from {@link StatusRepository}, reconstructs the
     * original scheduling payload, and republishes a {@link GenerationCreated} event.
     *
     * @param generationId the ID of the failed generation
     * @param errorResult the canonical error result from the failure
     * @return true if retry was triggered, false otherwise
     */
    public boolean tryRetryGeneration(String generationId, ErrorResult errorResult) {
        if (!config.isRetryEnabled()) {
            log.debug("Retry disabled globally, skipping retry for generation {}", generationId);
            return false;
        }

        // Check if error is retryable
        if (errorResult == null || !errorResult.isRetryable()) {
            log.debug(
                    "Error {} is not retryable, skipping retry for generation {}",
                    errorResult,
                    generationId);
            return false;
        }

        ErrorResult error = errorResult;

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
     * 2. The canonical error code is retryable ({@link ErrorResult#isRetryable()})
     * 3. Maximum retry attempts have not been exceeded
     *
     * When eligible, the method creates a new enhancement run through {@link RunManagement},
     * reloads the enhancement and its parent generation from {@link StatusRepository},
     * restores the previous-enhancement chain context, and republishes an
     * {@link EnhancementCreated} event.
     *
     * @param enhancementId the ID of the failed enhancement
     * @param errorResult the canonical error result from the failure
     * @return true if retry was triggered, false otherwise
     */
    public boolean tryRetryEnhancement(String enhancementId, ErrorResult errorResult) {
        if (!config.isRetryEnabled()) {
            log.debug("Retry disabled globally, skipping retry for enhancement {}", enhancementId);
            return false;
        }

        // Check if error is retryable
        if (errorResult == null || !errorResult.isRetryable()) {
            log.debug(
                    "Error {} is not retryable, skipping retry for enhancement {}",
                    errorResult,
                    enhancementId);
            return false;
        }

        ErrorResult error = errorResult;

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
     * Finds the enhancement that precedes the target enhancement in the execution chain.
     *
     * This preserves sequential enhancement context when rebuilding a retry event.
     * Returns {@code null} when the target enhancement is the first item in the chain
     * (index {@code 0}).
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
