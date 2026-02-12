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

@QuarkusTest
public class PanacheStatusRepositoryTest {
    private static final int PAGE_SIZE = 5;

    private static final int NUM_RECORDS = 10;

    @Inject
    PanacheStatusRepository statusRepository;

    @Test
    @TestTransaction
    void testSaveAndRetrieveRequest() {
        RequestRecord requestRecord = new RequestRecord();
        requestRecord.setId(UUID.randomUUID().toString()); // Set ID explicitly
        requestRecord.setStatus(RequestStatus.RECEIVED);
        Instant now = Instant.now();
        requestRecord.setCreationDate(now);

        statusRepository.saveRequestRecord(requestRecord);

        RequestRecord retrieved = statusRepository.findRequestById(requestRecord.getId());
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(requestRecord.getId());
        assertThat(retrieved.getStatus()).isEqualTo(RequestStatus.RECEIVED);
        assertThat(retrieved.getCreationDate()).isNotNull();
    }

    @Test
    @TestTransaction
    void testPagingAndMapStruct() {
        // Clean up or account for existing data
        long initialCount = statusRepository.findAllRequests(0, 100).getTotalHits();

        IntStream.range(0, NUM_RECORDS).forEach(i -> {
            RequestRecord r = new RequestRecord();
            r.setId(UUID.randomUUID().toString());
            statusRepository.saveRequestRecord(r);
        });

        Page<RequestRecord> requestRecordPage = statusRepository.findAllRequests(0, PAGE_SIZE);
        assertThat(requestRecordPage.getContent()).hasSize(PAGE_SIZE);
        assertThat(requestRecordPage.getTotalHits()).isEqualTo(initialCount + NUM_RECORDS);
    }

    @Test
    @TestTransaction
    void testUpdateGeneration() {
        GenerationRecord generationRecord = new GenerationRecord();
        generationRecord.setId(UUID.randomUUID().toString());
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
        enhancementRecord.setId(UUID.randomUUID().toString());
        enhancementRecord.setEnhancerName("enhancerName1");
        enhancementRecord.setStatus(EnhancementStatus.NEW);
        enhancementRecord.setEnhancedSbomUrls(List.of("https://url1"));

        statusRepository.saveEnhancement(enhancementRecord);

        enhancementRecord.setEnhancerName("enhancerName2");
        enhancementRecord.setStatus(EnhancementStatus.FINISHED);
        enhancementRecord.setEnhancedSbomUrls(List.of("https://url1", "https://url2"));

        statusRepository.updateEnhancement(enhancementRecord);

        EnhancementRecord updated = statusRepository.findEnhancementById(enhancementRecord.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.getEnhancerName()).isEqualTo("enhancerName2");
        assertThat(updated.getStatus()).isEqualTo(EnhancementStatus.FINISHED);
        assertThat(updated.getEnhancedSbomUrls()).containsExactly("https://url1", "https://url2");
    }

    @Test
    @TestTransaction
    void testUpdateMissingGeneration() {
        String missingId = UUID.randomUUID().toString();
        GenerationRecord generationRecord = new GenerationRecord();
        generationRecord.setId(missingId);
        generationRecord.setGeneratorName("shouldNotExist");

        statusRepository.updateGeneration(generationRecord);

        GenerationRecord updated = statusRepository.findGenerationById(missingId);
        assertThat(updated).isNull();
    }

    @Test
    @TestTransaction
    void testUpdateMissingEnhancement() {
        String missingId = UUID.randomUUID().toString();
        EnhancementRecord enhancementRecord = new EnhancementRecord();
        enhancementRecord.setId(missingId);
        enhancementRecord.setEnhancerName("shouldNotExist");

        statusRepository.updateEnhancement(enhancementRecord);

        EnhancementRecord updated = statusRepository.findEnhancementById(missingId);
        assertThat(updated).isNull();
    }

    @Test
    @TestTransaction
    void testUpdateGenerationWithEnhancements() {
        RequestRecord requestRecord = new RequestRecord();
        requestRecord.setId(UUID.randomUUID().toString());
        statusRepository.saveRequestRecord(requestRecord);

        GenerationRecord initialGenerationRecord = new GenerationRecord();
        initialGenerationRecord.setId(UUID.randomUUID().toString());
        initialGenerationRecord.setRequestId(requestRecord.getId());

        EnhancementRecord enhancementRecord = new EnhancementRecord();
        enhancementRecord.setId(UUID.randomUUID().toString());
        enhancementRecord.setEnhancerName("enahancerName1");
        enhancementRecord.setEnhancedSbomUrls(List.of("https://url1"));
        enhancementRecord.setRequestId(requestRecord.getId());
        enhancementRecord.setGenerationId(initialGenerationRecord.getId()); // Explicit link

        initialGenerationRecord.setEnhancements(List.of(enhancementRecord));
        statusRepository.saveGeneration(initialGenerationRecord);

        // Update phase
        GenerationRecord updateGenerationRecord = new GenerationRecord();
        updateGenerationRecord.setId(initialGenerationRecord.getId());
        updateGenerationRecord.setGeneratorName("updatedGeneratorName");
        updateGenerationRecord.setRequestId(requestRecord.getId());

        EnhancementRecord enhancementRecord2 = new EnhancementRecord();
        enhancementRecord2.setId(UUID.randomUUID().toString());
        enhancementRecord2.setEnhancerName("enahancerName2");
        enhancementRecord2.setEnhancedSbomUrls(List.of("https://url2"));
        enhancementRecord2.setRequestId(requestRecord.getId());
        enhancementRecord2.setGenerationId(initialGenerationRecord.getId());

        updateGenerationRecord.setEnhancements(List.of(enhancementRecord2));

        // Save the new enhancement first (or let mergeEnhancements handle it)
        // In our logic, mergeEnhancements handles creation if it doesn't exist.

        statusRepository.updateGeneration(updateGenerationRecord);

        GenerationRecord updated = statusRepository.findGenerationById(updateGenerationRecord.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.getGeneratorName()).isEqualTo("updatedGeneratorName");
        assertThat(updated.getEnhancements()).hasSize(1);

        // Note: mergeEnhancements replaces the list, so only enhancementRecord2 should be there
        assertThat(updated.getEnhancements()).element(0).extracting("id").isEqualTo(enhancementRecord2.getId());
        assertThat(updated.getEnhancements()).element(0).extracting("enhancerName").isEqualTo("enahancerName2");
    }
}
