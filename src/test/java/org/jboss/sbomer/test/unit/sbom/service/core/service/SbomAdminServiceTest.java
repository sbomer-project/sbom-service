package org.jboss.sbomer.test.unit.sbom.service.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.jboss.sbomer.events.orchestration.EnhancementCreated;
import org.jboss.sbomer.events.orchestration.GenerationCreated;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.RunState;
import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;
import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;
import org.jboss.sbomer.sbom.service.core.port.api.RunManagement;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;
import org.jboss.sbomer.sbom.service.core.port.spi.enhancement.EnhancementScheduler;
import org.jboss.sbomer.sbom.service.core.port.spi.generation.GenerationScheduler;
import org.jboss.sbomer.sbom.service.core.service.SbomAdminService;
import org.jboss.sbomer.sbom.service.core.service.SbomMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SbomAdminServiceTest {

    @InjectMocks
    private SbomAdminService sbomAdminService;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private GenerationScheduler generationScheduler;

    @Mock
    private EnhancementScheduler enhancementScheduler;

    @Mock
    private SbomMapper sbomMapper;

    @Mock
    private RunManagement runManagement;

    @Test
    void testRetryGenerationNotFound() {
        // RunManagement will throw EntityNotFoundException when generation not found
        when(runManagement.retryGeneration("gen-1"))
                .thenThrow(new EntityNotFoundException("Generation not found: gen-1"));
        
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> sbomAdminService.retryGeneration("gen-1"));
        assertEquals("Generation not found: gen-1", ex.getMessage());
    }

    @Test
    void testRetryGenerationNotFailed() {
        // RunManagement will throw InvalidRetryStateException when generation is not FAILED
        when(runManagement.retryGeneration("gen-1"))
                .thenThrow(new InvalidRetryStateException(
                        "Generation must be in FAILED state to retry. Current state: GENERATING"));
        
        InvalidRetryStateException ex = assertThrows(InvalidRetryStateException.class,
                () -> sbomAdminService.retryGeneration("gen-1"));
        assertEquals("Generation must be in FAILED state to retry. Current state: GENERATING",
                ex.getMessage());
    }

    @Test
    void testRetryGenerationSuccess() {
        GenerationRecord record = new GenerationRecord();
        record.setId("gen-1");
        record.setStatus(GenerationStatus.FAILED);
        record.setRequestId("req-1");
        
        GenerationRunRecord newRun = new GenerationRunRecord();
        newRun.setId("run-1");
        newRun.setGenerationId("gen-1");
        newRun.setAttemptNumber(2);
        newRun.setState(RunState.PENDING);
        
        GenerationCreated event = mock(GenerationCreated.class);
        
        // Mock RunManagement to return new run
        when(runManagement.retryGeneration("gen-1")).thenReturn(newRun);
        
        // After retry, the record should be updated by RunManagement
        GenerationRecord updatedRecord = new GenerationRecord();
        updatedRecord.setId("gen-1");
        updatedRecord.setStatus(GenerationStatus.GENERATING);
        updatedRecord.setRequestId("req-1");
        
        when(statusRepository.findGenerationById("gen-1")).thenReturn(updatedRecord);
        when(sbomMapper.toGenerationRequestSpec(any())).thenReturn(null);
        when(sbomMapper.toGenerationCreatedEvent(any(), any(), any())).thenReturn(event);
        
        sbomAdminService.retryGeneration("gen-1");
        
        verify(runManagement).retryGeneration("gen-1");
        verify(generationScheduler).schedule(event);
    }

    @Test
    void testRetryEnhancementNotFound() {
        // First call to get enhancement record
        when(statusRepository.findEnhancementById("enh-1")).thenReturn(null);
        
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> sbomAdminService.retryEnhancement("enh-1"));
        assertEquals("Enhancement with ID enh-1 not found", ex.getMessage());
    }

    @Test
    void testRetryEnhancementNotFailed() {
        EnhancementRecord record = new EnhancementRecord();
        record.setId("enh-1");
        record.setStatus(EnhancementStatus.ENHANCING);
        record.setGenerationId("gen-1");
        
        when(statusRepository.findEnhancementById("enh-1")).thenReturn(record);
        when(statusRepository.findGenerationById("gen-1")).thenReturn(new GenerationRecord());
        
        // RunManagement will throw InvalidRetryStateException when enhancement is not FAILED
        when(runManagement.retryEnhancement("enh-1"))
                .thenThrow(new InvalidRetryStateException(
                        "Enhancement must be in FAILED state to retry. Current state: ENHANCING"));
        
        InvalidRetryStateException ex = assertThrows(InvalidRetryStateException.class,
                () -> sbomAdminService.retryEnhancement("enh-1"));
        assertEquals("Enhancement must be in FAILED state to retry. Current state: ENHANCING",
                ex.getMessage());
    }

    @Test
    void testRetryEnhancementParentMissing() {
        EnhancementRecord record = new EnhancementRecord();
        record.setId("enh-1");
        record.setStatus(EnhancementStatus.FAILED);
        record.setGenerationId("gen-1");
        
        when(statusRepository.findEnhancementById("enh-1")).thenReturn(record);
        when(statusRepository.findGenerationById("gen-1")).thenReturn(null);
        
        InvalidRetryStateException ex = assertThrows(InvalidRetryStateException.class,
                () -> sbomAdminService.retryEnhancement("enh-1"));
        assertEquals("Cannot retry enhancement because parent generation is missing.", ex.getMessage());
    }

    @Test
    void testRetryEnhancementSuccess() {
        EnhancementRecord record = new EnhancementRecord();
        record.setId("enh-2");
        record.setStatus(EnhancementStatus.FAILED);
        record.setGenerationId("gen-1");
        record.setIndex(1);
        
        EnhancementRunRecord newRun = new EnhancementRunRecord();
        newRun.setId("run-2");
        newRun.setEnhancementId("enh-2");
        newRun.setAttemptNumber(2);
        newRun.setState(RunState.PENDING);
        
        EnhancementRecord previousEnhancement = new EnhancementRecord();
        previousEnhancement.setId("enh-1");
        previousEnhancement.setIndex(0);
        
        GenerationRecord parentGeneration = new GenerationRecord();
        parentGeneration.setId("gen-1");
        parentGeneration.setEnhancements(List.of(previousEnhancement, record));
        
        EnhancementCreated event = mock(EnhancementCreated.class);
        
        // First call returns the original record
        when(statusRepository.findEnhancementById("enh-2")).thenReturn(record);
        when(statusRepository.findGenerationById("gen-1")).thenReturn(parentGeneration);
        
        // Mock RunManagement to return new run
        when(runManagement.retryEnhancement("enh-2")).thenReturn(newRun);
        
        // After retry, the record should be updated by RunManagement
        EnhancementRecord updatedRecord = new EnhancementRecord();
        updatedRecord.setId("enh-2");
        updatedRecord.setStatus(EnhancementStatus.ENHANCING);
        updatedRecord.setGenerationId("gen-1");
        updatedRecord.setIndex(1);
        
        // Second call (after retry) returns updated record
        when(statusRepository.findEnhancementById("enh-2")).thenReturn(record, updatedRecord);
        when(sbomMapper.toEnhancementCreatedEvent(any(), any(), any())).thenReturn(event);
        
        sbomAdminService.retryEnhancement("enh-2");
        
        verify(runManagement).retryEnhancement("enh-2");
        verify(enhancementScheduler).schedule(event);
    }
}
