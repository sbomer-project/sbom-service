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
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;
import org.jboss.sbomer.sbom.service.core.domain.exception.EntityNotFoundException;
import org.jboss.sbomer.sbom.service.core.domain.exception.InvalidRetryStateException;
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

    @Test
    void testRetryGenerationNotFound() {
        when(statusRepository.findGenerationById("gen-1")).thenReturn(null);
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> sbomAdminService.retryGeneration("gen-1"));
        assertEquals("Generation with ID gen-1 not found", ex.getMessage());
    }

    @Test
    void testRetryGenerationNotFailed() {
        GenerationRecord record = new GenerationRecord();
        record.setId("gen-1");
        record.setStatus(GenerationStatus.GENERATING);
        when(statusRepository.findGenerationById("gen-1")).thenReturn(record);
        InvalidRetryStateException ex = assertThrows(InvalidRetryStateException.class,
                () -> sbomAdminService.retryGeneration("gen-1"));
        assertEquals("Cannot retry generation in status: GENERATING. Only FAILED generations can be retried.",
                ex.getMessage());
    }

    @Test
    void testRetryGenerationSuccess() {
        GenerationRecord record = new GenerationRecord();
        record.setId("gen-1");
        record.setStatus(GenerationStatus.FAILED);
        record.setRequestId("req-1");
        GenerationCreated event = mock(GenerationCreated.class);
        when(statusRepository.findGenerationById("gen-1")).thenReturn(record);
        when(sbomMapper.toGenerationRequestSpec(any())).thenReturn(null);
        when(sbomMapper.toGenerationCreatedEvent(any(), any(), any())).thenReturn(event);
        sbomAdminService.retryGeneration("gen-1");
        assertEquals(GenerationStatus.NEW, record.getStatus());
        verify(statusRepository).updateGeneration(record);
        verify(generationScheduler).schedule(event);
    }

    @Test
    void testRetryEnhancementNotFound() {
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
        when(statusRepository.findEnhancementById("enh-1")).thenReturn(record);
        InvalidRetryStateException ex = assertThrows(InvalidRetryStateException.class,
                () -> sbomAdminService.retryEnhancement("enh-1"));
        assertEquals("Cannot retry enhancement in status: ENHANCING. Only FAILED enhancements can be retried.",
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
        EnhancementRecord previousEnhancement = new EnhancementRecord();
        previousEnhancement.setId("enh-1");
        previousEnhancement.setIndex(0);
        GenerationRecord parentGeneration = new GenerationRecord();
        parentGeneration.setId("gen-1");
        parentGeneration.setEnhancements(List.of(previousEnhancement, record));
        EnhancementCreated event = mock(EnhancementCreated.class);
        when(statusRepository.findEnhancementById("enh-2")).thenReturn(record);
        when(statusRepository.findGenerationById("gen-1")).thenReturn(parentGeneration);
        when(sbomMapper.toEnhancementCreatedEvent(record, previousEnhancement, parentGeneration)).thenReturn(event);
        sbomAdminService.retryEnhancement("enh-2");
        assertEquals(EnhancementStatus.NEW, record.getStatus());
        verify(statusRepository).updateEnhancement(record);
        verify(enhancementScheduler).schedule(event);
    }
}
