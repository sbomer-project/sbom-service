package org.jboss.sbomer.test.unit.sbom.service.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.RunState;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;
import org.jboss.sbomer.sbom.service.core.service.RunManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test to verify that upstreamReason is properly set when completing runs with failures.
 * This addresses the bug where error reasons were not appearing in the UI.
 */
@ExtendWith(MockitoExtension.class)
public class RunManagementUpstreamReasonTest {

    @InjectMocks
    private RunManagementService runManagementService;

    @Mock
    private StatusRepository repository;

    @Captor
    private ArgumentCaptor<GenerationRunRecord> generationRunCaptor;

    @Captor
    private ArgumentCaptor<EnhancementRunRecord> enhancementRunCaptor;

    @Test
    void testCompleteGenerationRun_WithFailure_ShouldSetUpstreamReason() {
        // Given
        String runId = "run-123";
        String generationId = "gen-456";
        ErrorResult errorResult = ErrorResult.GENERATOR_EXECUTION_FAILED;
        String upstreamReason = "TaskRunFailed: pod terminated unexpectedly";

        GenerationRunRecord run = new GenerationRunRecord();
        run.setId(runId);
        run.setGenerationId(generationId);
        run.setState(RunState.RUNNING);

        GenerationRecord generation = new GenerationRecord();
        generation.setId(generationId);
        generation.setStatus(GenerationStatus.GENERATING);

        when(repository.findGenerationRunById(runId)).thenReturn(run);
        when(repository.findGenerationById(generationId)).thenReturn(generation);

        // When
        runManagementService.completeGenerationRun(runId, errorResult, upstreamReason);

        // Then
        verify(repository).updateGenerationRun(generationRunCaptor.capture());
        GenerationRunRecord updatedRun = generationRunCaptor.getValue();

        assertThat(updatedRun.getState()).isEqualTo(RunState.FAILED);
        assertThat(updatedRun.getErrorResult()).isEqualTo(errorResult);
        assertThat(updatedRun.getMessage()).isEqualTo(upstreamReason);
        assertThat(updatedRun.getUpstreamReason()).isEqualTo(upstreamReason);
        assertThat(updatedRun.getCompletionTime()).isNotNull();
    }

    @Test
    void testCompleteGenerationRun_WithSuccess_ShouldNotSetUpstreamReason() {
        // Given
        String runId = "run-123";
        String generationId = "gen-456";
        String message = "Generation completed successfully";

        GenerationRunRecord run = new GenerationRunRecord();
        run.setId(runId);
        run.setGenerationId(generationId);
        run.setState(RunState.RUNNING);

        GenerationRecord generation = new GenerationRecord();
        generation.setId(generationId);
        generation.setStatus(GenerationStatus.GENERATING);

        when(repository.findGenerationRunById(runId)).thenReturn(run);
        when(repository.findGenerationById(generationId)).thenReturn(generation);

        // When
        runManagementService.completeGenerationRun(runId, null, message);

        // Then
        verify(repository).updateGenerationRun(generationRunCaptor.capture());
        GenerationRunRecord updatedRun = generationRunCaptor.getValue();

        assertThat(updatedRun.getState()).isEqualTo(RunState.SUCCEEDED);
        assertThat(updatedRun.getErrorResult()).isNull();
        assertThat(updatedRun.getMessage()).isEqualTo(message);
        assertThat(updatedRun.getUpstreamReason()).isNull();
        assertThat(updatedRun.getCompletionTime()).isNotNull();
    }

    @Test
    void testCompleteEnhancementRun_WithFailure_ShouldSetUpstreamReason() {
        // Given
        String runId = "run-789";
        String enhancementId = "enh-012";
        ErrorResult errorResult = ErrorResult.ENHANCER_EXECUTION_FAILED;
        String upstreamReason = "TaskRunFailed: timeout exceeded";

        EnhancementRunRecord run = new EnhancementRunRecord();
        run.setId(runId);
        run.setEnhancementId(enhancementId);
        run.setState(RunState.RUNNING);

        EnhancementRecord enhancement = new EnhancementRecord();
        enhancement.setId(enhancementId);
        enhancement.setStatus(EnhancementStatus.ENHANCING);

        when(repository.findEnhancementRunById(runId)).thenReturn(run);
        when(repository.findEnhancementById(enhancementId)).thenReturn(enhancement);

        // When
        runManagementService.completeEnhancementRun(runId, errorResult, upstreamReason);

        // Then
        verify(repository).updateEnhancementRun(enhancementRunCaptor.capture());
        EnhancementRunRecord updatedRun = enhancementRunCaptor.getValue();

        assertThat(updatedRun.getState()).isEqualTo(RunState.FAILED);
        assertThat(updatedRun.getErrorResult()).isEqualTo(errorResult);
        assertThat(updatedRun.getMessage()).isEqualTo(upstreamReason);
        assertThat(updatedRun.getUpstreamReason()).isEqualTo(upstreamReason);
        assertThat(updatedRun.getCompletionTime()).isNotNull();
    }

    @Test
    void testCompleteEnhancementRun_WithSuccess_ShouldNotSetUpstreamReason() {
        // Given
        String runId = "run-789";
        String enhancementId = "enh-012";
        String message = "Enhancement completed successfully";

        EnhancementRunRecord run = new EnhancementRunRecord();
        run.setId(runId);
        run.setEnhancementId(enhancementId);
        run.setState(RunState.RUNNING);

        EnhancementRecord enhancement = new EnhancementRecord();
        enhancement.setId(enhancementId);
        enhancement.setStatus(EnhancementStatus.ENHANCING);

        when(repository.findEnhancementRunById(runId)).thenReturn(run);
        when(repository.findEnhancementById(enhancementId)).thenReturn(enhancement);

        // When
        runManagementService.completeEnhancementRun(runId, null, message);

        // Then
        verify(repository).updateEnhancementRun(enhancementRunCaptor.capture());
        EnhancementRunRecord updatedRun = enhancementRunCaptor.getValue();

        assertThat(updatedRun.getState()).isEqualTo(RunState.SUCCEEDED);
        assertThat(updatedRun.getErrorResult()).isNull();
        assertThat(updatedRun.getMessage()).isEqualTo(message);
        assertThat(updatedRun.getUpstreamReason()).isNull();
        assertThat(updatedRun.getCompletionTime()).isNotNull();
    }
}

