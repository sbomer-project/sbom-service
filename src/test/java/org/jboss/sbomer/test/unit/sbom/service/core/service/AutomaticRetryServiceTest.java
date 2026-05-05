package org.jboss.sbomer.test.unit.sbom.service.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.jboss.sbomer.events.common.GenerationRequestSpec;
import org.jboss.sbomer.events.orchestration.EnhancementCreated;
import org.jboss.sbomer.events.orchestration.GenerationCreated;
import org.jboss.sbomer.sbom.service.core.config.RetryPolicyConfig;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.RunState;
import org.jboss.sbomer.sbom.service.core.port.api.RunManagement;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;
import org.jboss.sbomer.sbom.service.core.port.spi.enhancement.EnhancementScheduler;
import org.jboss.sbomer.sbom.service.core.port.spi.generation.GenerationScheduler;
import org.jboss.sbomer.sbom.service.core.service.AutomaticRetryService;
import org.jboss.sbomer.sbom.service.core.service.SbomMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AutomaticRetryService}.
 *
 * Tests verify:
 * - Retry logic for retryable errors
 * - No retry for non-retryable errors
 * - No retry when globally disabled
 * - Max attempts enforcement
 * - Successful retry triggering
 * - Error handling during retry
 */
@ExtendWith(MockitoExtension.class)
class AutomaticRetryServiceTest {

    @Mock
    private RetryPolicyConfig config;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private RunManagement runManagement;

    @Mock
    private GenerationScheduler generationScheduler;

    @Mock
    private EnhancementScheduler enhancementScheduler;

    @Mock
    private SbomMapper sbomMapper;

    @InjectMocks
    private AutomaticRetryService automaticRetryService;

    private static final String TEST_GENERATION_ID = "test-generation-123";
    private static final String TEST_ENHANCEMENT_ID = "test-enhancement-456";

    @BeforeEach
    void setUp() {
        // Default: retry enabled
        lenient().when(config.isRetryEnabled()).thenReturn(true);
        
        // Set up default mocks for generation retry flow (lenient because not all tests use these)
        GenerationRecord mockGenerationRecord = new GenerationRecord();
        mockGenerationRecord.setId(TEST_GENERATION_ID);
        mockGenerationRecord.setRequestId("test-request-123");
        
        GenerationRequestSpec mockSpec = mock(GenerationRequestSpec.class);
        GenerationCreated mockGenerationEvent = mock(GenerationCreated.class);
        
        lenient().when(statusRepository.findGenerationById(TEST_GENERATION_ID)).thenReturn(mockGenerationRecord);
        lenient().when(sbomMapper.toGenerationRequestSpec(any(GenerationRecord.class))).thenReturn(mockSpec);
        lenient().when(sbomMapper.toGenerationCreatedEvent(any(GenerationRecord.class), any(GenerationRequestSpec.class), any(String.class)))
                .thenReturn(mockGenerationEvent);
        
        // Set up default mocks for enhancement retry flow (lenient because not all tests use these)
        EnhancementRecord mockEnhancementRecord = new EnhancementRecord();
        mockEnhancementRecord.setId(TEST_ENHANCEMENT_ID);
        mockEnhancementRecord.setGenerationId("test-generation-123");
        mockEnhancementRecord.setIndex(0);
        
        EnhancementCreated mockEnhancementEvent = mock(EnhancementCreated.class);
        
        lenient().when(statusRepository.findEnhancementById(TEST_ENHANCEMENT_ID)).thenReturn(mockEnhancementRecord);
        lenient().when(statusRepository.findGenerationById("test-generation-123")).thenReturn(mockGenerationRecord);
        lenient().when(sbomMapper.toEnhancementCreatedEvent(any(EnhancementRecord.class), any(), any(GenerationRecord.class)))
                .thenReturn(mockEnhancementEvent);
    }

    // ==================== GENERATION RETRY TESTS ====================

    @Test
    void testRetryGeneration_WhenRetryDisabledGlobally_ShouldNotRetry() {
        // Given
        when(config.isRetryEnabled()).thenReturn(false);

        // When
        boolean result = automaticRetryService.tryRetryGeneration(
                TEST_GENERATION_ID,
                GenerationResult.ERR_SYSTEM);

        // Then
        assertFalse(result, "Should not retry when globally disabled");
        verify(config).isRetryEnabled();
        verify(statusRepository, never()).findGenerationRunsByGenerationId(any());
        verify(runManagement, never()).retryGeneration(any());
    }

    @Test
    void testRetryGeneration_WhenErrorNotRetryable_ShouldNotRetry() {
        // Given

        // When
        boolean result = automaticRetryService.tryRetryGeneration(
                TEST_GENERATION_ID,
                GenerationResult.ERR_CONFIG_INVALID);

        // Then
        assertFalse(result, "Should not retry non-retryable error");
        verify(config).isRetryEnabled();
        verify(statusRepository, never()).findGenerationRunsByGenerationId(any());
        verify(runManagement, never()).retryGeneration(any());
    }

    @Test
    void testRetryGeneration_WhenMaxAttemptsReached_ShouldNotRetry() {
        // Given
        when(config.getMaxAttemptsForGeneration(GenerationResult.ERR_SYSTEM)).thenReturn(3);

        // Mock 3 existing runs (max attempts reached)
        List<GenerationRunRecord> existingRuns = createGenerationRuns(3);
        when(statusRepository.findGenerationRunsByGenerationId(TEST_GENERATION_ID))
                .thenReturn(existingRuns);

        // When
        boolean result = automaticRetryService.tryRetryGeneration(
                TEST_GENERATION_ID,
                GenerationResult.ERR_SYSTEM);

        // Then
        assertFalse(result, "Should not retry when max attempts reached");
        verify(config).isRetryEnabled();
        verify(statusRepository).findGenerationRunsByGenerationId(TEST_GENERATION_ID);
        verify(config).getMaxAttemptsForGeneration(GenerationResult.ERR_SYSTEM);
        verify(runManagement, never()).retryGeneration(any());
    }

    @Test
    void testRetryGeneration_WhenBelowMaxAttempts_ShouldRetry() {
        // Given
        when(config.getMaxAttemptsForGeneration(GenerationResult.ERR_SYSTEM)).thenReturn(5);

        // Mock 2 existing runs (below max attempts)
        List<GenerationRunRecord> existingRuns = createGenerationRuns(2);
        when(statusRepository.findGenerationRunsByGenerationId(TEST_GENERATION_ID))
                .thenReturn(existingRuns);

        // When
        boolean result = automaticRetryService.tryRetryGeneration(
                TEST_GENERATION_ID,
                GenerationResult.ERR_SYSTEM);

        // Then
        assertTrue(result, "Should retry when below max attempts");
        verify(config).isRetryEnabled();
        verify(statusRepository).findGenerationRunsByGenerationId(TEST_GENERATION_ID);
        verify(config).getMaxAttemptsForGeneration(GenerationResult.ERR_SYSTEM);
        verify(runManagement).retryGeneration(TEST_GENERATION_ID);
        verify(statusRepository).findGenerationById(TEST_GENERATION_ID);
        verify(sbomMapper).toGenerationRequestSpec(any(GenerationRecord.class));
        verify(sbomMapper).toGenerationCreatedEvent(any(GenerationRecord.class), any(GenerationRequestSpec.class), any(String.class));
        verify(generationScheduler).schedule(any(GenerationCreated.class));
    }

    @Test
    void testRetryGeneration_WhenFirstAttemptFails_ShouldRetry() {
        // Given
        // Note: isRetryableGeneration is no longer called - retryability is determined by ErrorResult enum
        when(config.getMaxAttemptsForGeneration(GenerationResult.ERR_OOM)).thenReturn(3);

        // Mock 1 existing run (first attempt failed)
        List<GenerationRunRecord> existingRuns = createGenerationRuns(1);
        when(statusRepository.findGenerationRunsByGenerationId(TEST_GENERATION_ID))
                .thenReturn(existingRuns);

        // When
        boolean result = automaticRetryService.tryRetryGeneration(
                TEST_GENERATION_ID,
                GenerationResult.ERR_OOM);

        // Then
        assertTrue(result, "Should retry after first attempt failure");
        verify(runManagement).retryGeneration(TEST_GENERATION_ID);
        verify(statusRepository).findGenerationById(TEST_GENERATION_ID);
        verify(sbomMapper).toGenerationRequestSpec(any(GenerationRecord.class));
        verify(sbomMapper).toGenerationCreatedEvent(any(GenerationRecord.class), any(GenerationRequestSpec.class), any(String.class));
        verify(generationScheduler).schedule(any(GenerationCreated.class));
    }

    @Test
    void testRetryGeneration_WhenRetryThrowsException_ShouldReturnFalse() {
        // Given
        // Note: isRetryableGeneration is no longer called - retryability is determined by ErrorResult enum
        when(config.getMaxAttemptsForGeneration(GenerationResult.ERR_SYSTEM)).thenReturn(5);

        List<GenerationRunRecord> existingRuns = createGenerationRuns(1);
        when(statusRepository.findGenerationRunsByGenerationId(TEST_GENERATION_ID))
                .thenReturn(existingRuns);

        // Mock exception during retry
        doThrow(new RuntimeException("Retry failed")).when(runManagement).retryGeneration(TEST_GENERATION_ID);

        // When
        boolean result = automaticRetryService.tryRetryGeneration(
                TEST_GENERATION_ID,
                GenerationResult.ERR_SYSTEM);

        // Then
        assertFalse(result, "Should return false when retry throws exception");
        verify(runManagement).retryGeneration(TEST_GENERATION_ID);
    }

    // ==================== ENHANCEMENT RETRY TESTS ====================

    @Test
    void testRetryEnhancement_WhenRetryDisabledGlobally_ShouldNotRetry() {
        // Given
        when(config.isRetryEnabled()).thenReturn(false);

        // When
        boolean result = automaticRetryService.tryRetryEnhancement(
                TEST_ENHANCEMENT_ID,
                EnhancementResult.ERR_GENERAL);

        // Then
        assertFalse(result, "Should not retry when globally disabled");
        verify(config).isRetryEnabled();
        verify(statusRepository, never()).findEnhancementRunsByEnhancementId(any());
        verify(runManagement, never()).retryEnhancement(any());
    }

    @Test
    void testRetryEnhancement_WhenErrorNotRetryable_ShouldNotRetry() {
        // Given

        // When
        boolean result = automaticRetryService.tryRetryEnhancement(
                TEST_ENHANCEMENT_ID,
                EnhancementResult.ERR_CONFIG_INVALID);

        // Then
        assertFalse(result, "Should not retry non-retryable error");
        verify(config).isRetryEnabled();
        verify(statusRepository, never()).findEnhancementRunsByEnhancementId(any());
        verify(runManagement, never()).retryEnhancement(any());
    }

    @Test
    void testRetryEnhancement_WhenMaxAttemptsReached_ShouldNotRetry() {
        // Given
        when(config.getMaxAttemptsForEnhancement(EnhancementResult.ERR_ENHANCEMENT)).thenReturn(3);

        // Mock 3 existing runs (max attempts reached)
        List<EnhancementRunRecord> existingRuns = createEnhancementRuns(3);
        when(statusRepository.findEnhancementRunsByEnhancementId(TEST_ENHANCEMENT_ID))
                .thenReturn(existingRuns);

        // When
        boolean result = automaticRetryService.tryRetryEnhancement(
                TEST_ENHANCEMENT_ID,
                EnhancementResult.ERR_ENHANCEMENT);

        // Then
        assertFalse(result, "Should not retry when max attempts reached");
        verify(config).isRetryEnabled();
        verify(statusRepository).findEnhancementRunsByEnhancementId(TEST_ENHANCEMENT_ID);
        verify(config).getMaxAttemptsForEnhancement(EnhancementResult.ERR_ENHANCEMENT);
        verify(runManagement, never()).retryEnhancement(any());
    }

    @Test
    void testRetryEnhancement_WhenBelowMaxAttempts_ShouldRetry() {
        // Given
        when(config.getMaxAttemptsForEnhancement(EnhancementResult.ERR_GENERAL)).thenReturn(2);

        // Mock 1 existing run (below max attempts)
        List<EnhancementRunRecord> existingRuns = createEnhancementRuns(1);
        when(statusRepository.findEnhancementRunsByEnhancementId(TEST_ENHANCEMENT_ID))
                .thenReturn(existingRuns);

        // When
        boolean result = automaticRetryService.tryRetryEnhancement(
                TEST_ENHANCEMENT_ID,
                EnhancementResult.ERR_GENERAL);

        // Then
        assertTrue(result, "Should retry when below max attempts");
        verify(config).isRetryEnabled();
        verify(statusRepository).findEnhancementRunsByEnhancementId(TEST_ENHANCEMENT_ID);
        verify(config).getMaxAttemptsForEnhancement(EnhancementResult.ERR_GENERAL);
        verify(runManagement).retryEnhancement(TEST_ENHANCEMENT_ID);
        verify(statusRepository).findEnhancementById(TEST_ENHANCEMENT_ID);
        verify(statusRepository, atLeastOnce()).findGenerationById(any());
        verify(sbomMapper).toEnhancementCreatedEvent(any(EnhancementRecord.class), any(), any(GenerationRecord.class));
        verify(enhancementScheduler).schedule(any(EnhancementCreated.class));
    }

    @Test
    void testRetryEnhancement_WhenRetryThrowsException_ShouldReturnFalse() {
        // Given
        // Note: isRetryableEnhancement is no longer called - retryability is determined by ErrorResult enum
        when(config.getMaxAttemptsForEnhancement(EnhancementResult.ERR_ENHANCEMENT)).thenReturn(3);

        List<EnhancementRunRecord> existingRuns = createEnhancementRuns(1);
        when(statusRepository.findEnhancementRunsByEnhancementId(TEST_ENHANCEMENT_ID))
                .thenReturn(existingRuns);

        // Mock exception during retry
        doThrow(new RuntimeException("Retry failed")).when(runManagement).retryEnhancement(TEST_ENHANCEMENT_ID);

        // When
        boolean result = automaticRetryService.tryRetryEnhancement(
                TEST_ENHANCEMENT_ID,
                EnhancementResult.ERR_ENHANCEMENT);

        // Then
        assertFalse(result, "Should return false when retry throws exception");
        verify(runManagement).retryEnhancement(TEST_ENHANCEMENT_ID);
    }

    // ==================== HELPER METHODS ====================

    private List<GenerationRunRecord> createGenerationRuns(int count) {
        List<GenerationRunRecord> runs = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            GenerationRunRecord run = new GenerationRunRecord();
            run.setId("run-" + i);
            run.setGenerationId(TEST_GENERATION_ID);
            run.setAttemptNumber(i);
            run.setState(RunState.FAILED);
            run.setStartTime(Instant.now());
            run.setCompletionTime(Instant.now());
            runs.add(run);
        }
        return runs;
    }

    private List<EnhancementRunRecord> createEnhancementRuns(int count) {
        List<EnhancementRunRecord> runs = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            EnhancementRunRecord run = new EnhancementRunRecord();
            run.setId("run-" + i);
            run.setEnhancementId(TEST_ENHANCEMENT_ID);
            run.setAttemptNumber(i);
            run.setState(RunState.FAILED);
            run.setStartTime(Instant.now());
            run.setCompletionTime(Instant.now());
            runs.add(run);
        }
        return runs;
    }
}
