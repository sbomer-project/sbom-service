package org.jboss.sbomer.test.unit.sbom.service.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.jboss.sbomer.events.common.ContextSpec;
import org.jboss.sbomer.events.common.GenerationRequestSpec;
import org.jboss.sbomer.events.common.Target;
import org.jboss.sbomer.events.enhancer.EnhancementUpdate;
import org.jboss.sbomer.events.enhancer.EnhancementUpdateData;
import org.jboss.sbomer.events.generator.GenerationUpdate;
import org.jboss.sbomer.events.generator.GenerationUpdateData;
import org.jboss.sbomer.events.orchestration.GenerationCreated;
import org.jboss.sbomer.events.orchestration.RequestsFinished;
import org.jboss.sbomer.events.request.RequestData;
import org.jboss.sbomer.events.request.RequestsCreated;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRunRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.RequestStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.RunState;
import org.jboss.sbomer.sbom.service.core.port.api.RunManagement;
import org.jboss.sbomer.sbom.service.core.port.spi.FailureNotifier;
import org.jboss.sbomer.sbom.service.core.port.spi.RecipeBuilder;
import org.jboss.sbomer.sbom.service.core.port.spi.RequestsFinishedNotifier;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;
import org.jboss.sbomer.sbom.service.core.port.spi.enhancement.EnhancementScheduler;
import org.jboss.sbomer.sbom.service.core.port.spi.generation.GenerationScheduler;
import org.jboss.sbomer.sbom.service.core.service.SbomMapper;
import org.jboss.sbomer.sbom.service.core.service.SbomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SbomServiceTest {

    @InjectMocks
    private SbomService sbomService;

    @Mock
    private GenerationScheduler generationScheduler;

    @Mock
    private EnhancementScheduler enhancementScheduler;

    @Mock
    private SbomMapper sbomMapper;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private RecipeBuilder recipeBuilder;

    @Mock
    private RequestsFinishedNotifier requestsFinishedNotifier;

    @Mock
    private FailureNotifier failureNotifier;

    @Mock
    private RunManagement runManagement;

    @Captor
    private ArgumentCaptor<GenerationRecord> generationRecordCaptor;

    // Helper to satisfy Avro's strict requirements for the Context field
    private ContextSpec createDummyContext() {
        return ContextSpec.newBuilder()
            .setEventId("event-123")
            .setType("TestEvent")
            .setSource("TestSource")
            .setCorrelationId("corr-123")
            .setEventVersion("1.0")
            .setTimestamp(Instant.now())
            .build();
    }

    @Test
    void testProcessGenerations() {
        // Setup initial request
        Target dummyTarget = Target.newBuilder()
            .setType("purl")
            .setIdentifier("pkg:maven/test/test@1.0")
            .build();

        GenerationRequestSpec genSpec = GenerationRequestSpec.newBuilder()
            .setGenerationId("gen-123") // Required by Avro
            .setTarget(dummyTarget)     // Required by Avro/Mapper
            .build();

        RequestData reqData = RequestData.newBuilder()
            .setRequestId("req-123")
            .setGenerationRequests(List.of(genSpec))
            .build();

        RequestsCreated requestsCreated = RequestsCreated.newBuilder()
            .setContext(createDummyContext()) // Required by Avro
            .setData(reqData)
            .build();

        RequestRecord requestRecord = new RequestRecord();
        GenerationRecord generationRecord = new GenerationRecord();
        generationRecord.setId("gen-123");

        // Mock mapper behaviors using mock() instead of Avro builders
        GenerationCreated mockedGenerationCreated = mock(GenerationCreated.class);

        when(sbomMapper.toNewRequestRecord(requestsCreated)).thenReturn(requestRecord);
        when(sbomMapper.toNewGenerationRecord(eq(genSpec), eq("req-123"))).thenReturn(generationRecord);
        when(sbomMapper.toGenerationCreatedEvent(eq(generationRecord), eq(genSpec), eq("req-123")))
            .thenReturn(mockedGenerationCreated);

        // Execute
        sbomService.processGenerations(requestsCreated);

        // Verify repository saves
        verify(statusRepository).saveRequestRecord(requestRecord);
        verify(statusRepository).saveGeneration(generationRecord);
        verify(statusRepository).saveGenerationRun(any(GenerationRunRecord.class));

        // Verify scheduler and roll-up
        verify(generationScheduler).schedule(mockedGenerationCreated);
        verify(runManagement).rollUpGenerationsToRequest("req-123");
    }

    @Test
    void testProcessGenerationStatusUpdate_Finished_NoEnhancements() {
        // Setup data
        String generationId = "gen-123";
        String requestId = "req-123";
        String runId = "run-123";

        GenerationUpdateData updateData = GenerationUpdateData.newBuilder()
            .setGenerationId(generationId)
            .setStatus("FINISHED")
            .setResultCode(0)
            .setBaseSbomUrls(List.of("http://base-url.com"))
            .build();

        GenerationUpdate generationUpdate = GenerationUpdate.newBuilder()
            .setContext(createDummyContext()) // Required by Avro
            .setData(updateData)
            .build();

        GenerationRecord record = new GenerationRecord();
        record.setId(generationId);
        record.setRequestId(requestId);
        record.setEnhancements(Collections.emptyList()); // NO Enhancements

        GenerationRunRecord runRecord = new GenerationRunRecord();
        runRecord.setId(runId);
        runRecord.setState(RunState.RUNNING);

        RequestRecord requestRecord = new RequestRecord();
        RequestsFinished mockedRequestsFinished = mock(RequestsFinished.class);

        // Mocks
        when(statusRepository.findGenerationById(generationId)).thenReturn(record);
        when(statusRepository.findGenerationRunsByGenerationId(generationId)).thenReturn(List.of(runRecord));
        when(statusRepository.isGenerationAndEnhancementsFinished(generationId)).thenReturn(true);
        when(statusRepository.isAllGenerationRequestsFinished(requestId)).thenReturn(true);
        when(statusRepository.findRequestById(requestId)).thenReturn(requestRecord);
        when(sbomMapper.toRequestsFinishedEvent(requestRecord)).thenReturn(mockedRequestsFinished);

        // Execute
        sbomService.processGenerationStatusUpdate(generationUpdate);

        // Verify Run completion
        verify(runManagement).completeGenerationRun(runId, GenerationResult.SUCCESS, "Generation completed");

        // Verify Generation Update (Capturing the record to check finalSbomUrls)
        verify(statusRepository, times(2)).updateGeneration(generationRecordCaptor.capture());
        GenerationRecord updatedRecord = generationRecordCaptor.getAllValues().get(1);

        // Assert base URLs were used as final URLs since there are no enhancements
        assertThat(updatedRecord.getGenerationSbomUrls()).containsExactly("http://base-url.com");
        assertThat(updatedRecord.getFinalSbomUrls()).containsExactly("http://base-url.com");

        // Verify Request completion
        verify(statusRepository).updateRequestRecord(requestRecord);
        assertThat(requestRecord.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        verify(requestsFinishedNotifier).notify(mockedRequestsFinished);
    }

    @Test
    void testProcessEnhancementStatusUpdate_Finished_CompletesRequest() {
        // Setup data
        String enhancementId = "enh-123";
        String generationId = "gen-123";
        String requestId = "req-123";
        String runId = "run-123";

        EnhancementUpdateData updateData = EnhancementUpdateData.newBuilder()
            .setEnhancementId(enhancementId)
            .setStatus("FINISHED")
            .setResultCode(0)
            .setEnhancedSbomUrls(List.of("http://enhanced-url-2.com"))
            .build();

        EnhancementUpdate enhancementUpdate = EnhancementUpdate.newBuilder()
            .setContext(createDummyContext()) // Required by Avro
            .setData(updateData)
            .build();

        EnhancementRecord enh1 = new EnhancementRecord();
        enh1.setIndex(0);
        enh1.setEnhancedSbomUrls(List.of("http://enhanced-url-1.com"));

        EnhancementRecord enh2 = new EnhancementRecord();
        enh2.setId(enhancementId);
        enh2.setGenerationId(generationId);
        enh2.setRequestId(requestId);
        enh2.setIndex(1); // Higher index
        enh2.setEnhancedSbomUrls(List.of("http://enhanced-url-2.com"));

        GenerationRecord genRecord = new GenerationRecord();
        genRecord.setId(generationId);
        genRecord.setRequestId(requestId);
        genRecord.setGenerationSbomUrls(List.of("http://base-url.com"));
        genRecord.setEnhancements(List.of(enh1, enh2)); // HAS Enhancements

        EnhancementRunRecord runRecord = new EnhancementRunRecord();
        runRecord.setId(runId);
        runRecord.setState(RunState.RUNNING);

        RequestRecord requestRecord = new RequestRecord();
        RequestsFinished mockedRequestsFinished = mock(RequestsFinished.class);

        // Mocks
        when(statusRepository.findEnhancementById(enhancementId)).thenReturn(enh2);
        when(statusRepository.findEnhancementRunsByEnhancementId(enhancementId)).thenReturn(List.of(runRecord));
        when(statusRepository.findGenerationById(generationId)).thenReturn(genRecord);
        when(statusRepository.isGenerationAndEnhancementsFinished(generationId)).thenReturn(true);
        when(statusRepository.isAllGenerationRequestsFinished(requestId)).thenReturn(true);
        when(statusRepository.findRequestById(requestId)).thenReturn(requestRecord);
        when(sbomMapper.toRequestsFinishedEvent(requestRecord)).thenReturn(mockedRequestsFinished);

        // Execute
        sbomService.processEnhancementStatusUpdate(enhancementUpdate);

        // Verify Generation Update (Checking max index logic)
        verify(statusRepository).updateGeneration(generationRecordCaptor.capture());
        GenerationRecord updatedGenRecord = generationRecordCaptor.getValue();

        // Assert max-index enhanced URLs were used as final URLs
        assertThat(updatedGenRecord.getFinalSbomUrls()).containsExactly("http://enhanced-url-2.com");

        // Verify Request completion
        verify(requestsFinishedNotifier).notify(mockedRequestsFinished);
    }
}
