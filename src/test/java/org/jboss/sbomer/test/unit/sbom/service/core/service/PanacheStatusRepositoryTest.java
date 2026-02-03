package org.jboss.sbomer.test.unit.sbom.service.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.jboss.sbomer.sbom.service.adapter.in.rest.model.Page;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.PanacheStatusRepository;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.RequestStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@Transactional
public class PanacheStatusRepositoryTest {
    private static final int PAGE_SIZE = 5;

    private static final int NUM_RECORDS = 10;

    // XXX: Can we inject StatusRepository?
    @Inject
    PanacheStatusRepository statusRepository;

    @Test
    @TestTransaction
    void testSaveAndRetrieveRequest() {
        RequestRecord requestRecord = new RequestRecord();
        requestRecord.setStatus(RequestStatus.RECEIVED);
        Instant now = Instant.now();
        requestRecord.setCreationDate(now);
        statusRepository.saveRequestRecord(requestRecord);
        RequestRecord statusRepositoryRequestById = statusRepository.findRequestById(requestRecord.getId());
        assertThat(statusRepositoryRequestById).isNotNull();
        assertThat(statusRepositoryRequestById.getId()).isEqualTo(requestRecord.getId());
        assertThat(statusRepositoryRequestById.getStatus()).isEqualTo(RequestStatus.RECEIVED);
        assertThat(statusRepositoryRequestById.getCreationDate()).isAfterOrEqualTo(now);
    }

  @Test
    @TestTransaction
    void testPagingAndMapStruct() {
        long initialCount = statusRepository.findAllRequests(0, 1).getTotalHits();
        IntStream.range(0, NUM_RECORDS).forEach(i -> statusRepository.saveRequestRecord(new RequestRecord()));
        Page<RequestRecord> requestRecordPage = statusRepository.findAllRequests(0, PAGE_SIZE);
        assertThat(requestRecordPage.getContent()).hasSize(PAGE_SIZE);
        assertThat(requestRecordPage.getTotalHits()).isEqualTo(initialCount + NUM_RECORDS);
    }

    @Test
    @TestTransaction
    void testUpdateGeneration() {
        GenerationRecord generationRecord = new GenerationRecord();
        generationRecord.setGeneratorName("generatorName1");
        generationRecord.setStatus(GenerationStatus.NEW);
        generationRecord.setGenerationSbomUrls(List.of("https://url1"));
        statusRepository.saveGeneration(generationRecord);
        generationRecord.setGeneratorName("generatorName2");
        generationRecord.setStatus(GenerationStatus.FINISHED);
        generationRecord.setGenerationSbomUrls(List.of("https://url1", "https://url2"));
        statusRepository.updateGeneration(generationRecord);
        GenerationRecord updated = statusRepository.findGenerationById(generationRecord.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.getGeneratorName()).isEqualTo("generatorName2");
        assertThat(updated.getStatus()).isEqualTo(GenerationStatus.FINISHED);
        assertThat(updated.getGenerationSbomUrls()).containsExactly("https://url1", "https://url2");
    }

    @Test
    @TestTransaction
    void testUpdateEnhancement() {
        EnhancementRecord enhancementRecord = new EnhancementRecord();
        enhancementRecord.setEnhancerName("enhancerName1");
        enhancementRecord.setStatus(EnhancementStatus.NEW);
        enhancementRecord.setEnhancedSbomUrls(List.of("https://url1"));
        statusRepository.saveEnhancement(enhancementRecord);
        enhancementRecord.setEnhancerName("enhancerName2");
        enhancementRecord.setStatus(EnhancementStatus.FINISHED);
        enhancementRecord.setEnhancedSbomUrls(List.of("https://url1", "https://url2"));
        statusRepository.updateEnhancement(enhancementRecord);
        EnhancementRecord updatedEnhancementRecord = statusRepository.findEnhancementById(enhancementRecord.getId());
        assertThat(updatedEnhancementRecord).isNotNull();
        assertThat(updatedEnhancementRecord.getEnhancerName()).isEqualTo("enhancerName2");
        assertThat(updatedEnhancementRecord.getStatus()).isEqualTo(EnhancementStatus.FINISHED);
        assertThat(updatedEnhancementRecord.getEnhancedSbomUrls()).containsExactly("https://url1", "https://url2");
    }

    @Test
    @TestTransaction
    void testUpdateMissingGeneration() {
        String requestRecordId = UUID.randomUUID().toString();
        GenerationRecord generationRecord = new GenerationRecord();
        generationRecord.setId(requestRecordId);
        generationRecord.setGeneratorName("shouldNotExist");
        generationRecord.setStatus(GenerationStatus.NEW);
        statusRepository.updateGeneration(generationRecord);
        GenerationRecord updatedGenerationRecord = statusRepository.findGenerationById(requestRecordId);
        assertThat(updatedGenerationRecord).isNull();
    }

    @Test
    @TestTransaction
    void testUpdateMissingEnhancement() {
        String requestRecordId = UUID.randomUUID().toString();
        EnhancementRecord enhancementRecord = new EnhancementRecord();
        enhancementRecord.setId(requestRecordId);
        enhancementRecord.setEnhancerName("shouldNotExist");
        enhancementRecord.setStatus(EnhancementStatus.NEW);
        statusRepository.updateEnhancement(enhancementRecord);
        EnhancementRecord updatedEnhancementRecord = statusRepository.findEnhancementById(requestRecordId);
        assertThat(updatedEnhancementRecord).isNull();
    }

    @Test
    @TestTransaction
    void testUpdateGenerationWithEnhancements() {
        RequestRecord requestRecord = new RequestRecord();
        statusRepository.saveRequestRecord(requestRecord);
        GenerationRecord initialGenerationRecord = new GenerationRecord();
        EnhancementRecord enhancementRecord = new EnhancementRecord();
        enhancementRecord.setEnhancerName("enahancerName1");
        enhancementRecord.setEnhancedSbomUrls(List.of("https://url1"));
        enhancementRecord.setRequestId(requestRecord.getId());
        initialGenerationRecord.setEnhancements(List.of(enhancementRecord));
        statusRepository.saveGeneration(initialGenerationRecord);
        GenerationRecord updateGenerationRecord = new GenerationRecord();
        updateGenerationRecord.setId(initialGenerationRecord.getId());
        updateGenerationRecord.setGeneratorName("updatedGeneratorName");
        EnhancementRecord enhancementRecord2 = new EnhancementRecord();
        enhancementRecord2.setEnhancerName("enahancerName2");
        enhancementRecord2.setEnhancedSbomUrls(List.of("https://url2"));
        enhancementRecord2.setRequestId(requestRecord.getId());
        updateGenerationRecord.setEnhancements(List.of(enhancementRecord2));
        statusRepository.saveEnhancement(enhancementRecord2);
        EnhancementRecord enhancementRecord3 = statusRepository.findEnhancementById(enhancementRecord2.getId());
        assertThat(enhancementRecord3).isNotNull();
        enhancementRecord3.setGenerationId(initialGenerationRecord.getId());
        assertThat(enhancementRecord3.getGenerationId()).isEqualTo(updateGenerationRecord.getId());
        statusRepository.updateGeneration(updateGenerationRecord);
        GenerationRecord updatedGenerationRecord = statusRepository.findGenerationById(updateGenerationRecord.getId());
        assertThat(updatedGenerationRecord).isNotNull();
        assertThat(updatedGenerationRecord.getGeneratorName()).isEqualTo("updatedGeneratorName");
        assertThat(updatedGenerationRecord.getEnhancements()).hasSize(1);
        assertThat(updatedGenerationRecord.getEnhancements()).element(0).extracting("id").isEqualTo(enhancementRecord2.getId());
        assertThat(updatedGenerationRecord.getEnhancements()).element(0).extracting("enhancerName").isEqualTo("enahancerName2");
    }

    // This test checks whether saveRequestRecord successfully saves a request that already has an ID defined
    @Test
    @TestTransaction
    void testSaveRequestWithExistingId() {
        RequestRecord requestRecord = new RequestRecord();
        requestRecord.setId("dummy-id-123");
        statusRepository.saveRequestRecord(requestRecord);
    }

    // This test checks whether saveGeneration successfully saves a generation that already has an ID defined
    @Test
    @TestTransaction
    void testSaveGenerationWithExistingId() {
        GenerationRecord generationRecord = new GenerationRecord();
        generationRecord.setId("dummy-id-123");
        statusRepository.saveGeneration(generationRecord);
    }

    // This test checks whether saveEnhancement successfully saves a enhancement that already has an ID defined
    @Test
    @TestTransaction
    void testSaveEnhancementWithExistingId() {
        EnhancementRecord enhancementRecord = new EnhancementRecord();
        enhancementRecord.setId("dummy-id-123");
        statusRepository.saveEnhancement(enhancementRecord);
    }

    @Test
    @TestTransaction
    void testSaveRequestWithChildren() {
        // Create Parent
        RequestRecord request = new RequestRecord();
        request.setId("req-1");

        // Create Child
        GenerationRecord gen = new GenerationRecord();
        gen.setId("gen-1");
        gen.setRequestId("req-1"); // Link back

        request.setGenerationRecords(List.of(gen));

        // This shouldn't fail
        statusRepository.saveRequestRecord(request);
    }
}
