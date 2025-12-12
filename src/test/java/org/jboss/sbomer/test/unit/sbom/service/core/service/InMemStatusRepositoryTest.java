package org.jboss.sbomer.test.unit.sbom.service.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;

import org.jboss.sbomer.sbom.service.adapter.in.rest.model.Page;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.SimpleInMemStatusRepository;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.RequestStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
public class InMemStatusRepositoryTest {
    private static final int PAGE_SIZE = 5;

    private static final int NUM_RECORDS = 10;

    // XXX: Can we inject StatusRepository?
    @Inject
    SimpleInMemStatusRepository statusRepository;

    @Test
    void testSaveAndRetrieveRequest() {
        var requestRecord = new RequestRecord();
        String requestId = UUID.randomUUID().toString();
        requestRecord.setId(requestId);
        requestRecord.setStatus(RequestStatus.NEW);
        Instant now = Instant.now();
        requestRecord.setCreationDate(now);
        statusRepository.saveRequestRecord(requestRecord);
        var statusRepositoryRequestById = statusRepository.findRequestById(requestId);
        assertThat(statusRepositoryRequestById).isNotNull();
        assertThat(statusRepositoryRequestById.getId()).isEqualTo(requestId);
        assertThat(statusRepositoryRequestById.getStatus()).isEqualTo(RequestStatus.NEW);
        assertThat(statusRepositoryRequestById.getCreationDate()).isAfterOrEqualTo(now);
    }

    @Test
    void testPagingAndMapStruct() {
        IntStream.range(0, NUM_RECORDS).forEach(i -> {
            RequestRecord requestRecord = new RequestRecord();
            requestRecord.setId(String.valueOf(i));
            statusRepository.saveRequestRecord(requestRecord);
        });
        Page<RequestRecord> requestRecordPage = statusRepository.findAllRequests(0, PAGE_SIZE);
        assertThat(requestRecordPage.getContent()).hasSize(PAGE_SIZE);
        assertThat(requestRecordPage.getTotalHits()).isEqualTo(NUM_RECORDS + 1);
    }
}
