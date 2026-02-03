package org.jboss.sbomer.test.unit.sbom.service.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.EnhancementEntity;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.GenerationEntity;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.RequestEntity;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper.EnhancementMapper;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper.GenerationMapper;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper.StatusMapper;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.RequestStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class MapperTest {
    @Inject
    GenerationMapper generationMapper;

    @Inject
    EnhancementMapper enhancementMapper;

    @Inject
    StatusMapper statusMapper;

    @Test
    void testRequestRecord() {
        RequestRecord requestRecord = new RequestRecord();
        String requestId = UUID.randomUUID().toString();
        requestRecord.setId(requestId);
        requestRecord.setStatus(RequestStatus.RECEIVED);
        requestRecord.setCreationDate(Instant.now());
        GenerationRecord generationRecord = new GenerationRecord();
        String generationId = UUID.randomUUID().toString();
        generationRecord.setId(generationId);
        String generationRequestId = UUID.randomUUID().toString();
        generationRecord.setRequestId(generationRequestId);
        requestRecord.setGenerationRecords(List.of(generationRecord));
        RequestEntity requestEntity = statusMapper.toEntity(requestRecord);
        assertThat(requestEntity.getId()).isEqualTo(requestRecord.getId());
        assertThat(requestEntity.getStatus()).isEqualTo(requestRecord.getStatus());
        assertThat(requestEntity.getGenerations()).hasSize(1);
        assertThat(requestEntity.getGenerations()).element(0).extracting("id").isEqualTo(generationId);
        RequestRecord statusMapperDto = statusMapper.toDto(requestEntity);
        assertThat(statusMapperDto.getId()).isEqualTo(requestRecord.getId());
        assertThat(statusMapperDto.getGenerationRecords()).hasSize(1);
        assertThat(statusMapperDto.getGenerationRecords()).element(0).extracting("id").isEqualTo(generationId);
    }

    @Test
    void testGenerationRecord() {
        GenerationRecord generationRecord = new GenerationRecord();
        String generationId = UUID.randomUUID().toString();
        generationRecord.setId(generationId);
        String requestId = UUID.randomUUID().toString();
        generationRecord.setRequestId(requestId);
        generationRecord.setTargetType("image");
        generationRecord.setGenerationSbomUrls(List.of("https://url1", "https://url2"));
        EnhancementRecord enhancementRecord = new EnhancementRecord();
        String enhancementId = UUID.randomUUID().toString();
        enhancementRecord.setId(enhancementId);
        enhancementRecord.setGenerationId(generationId);
        generationRecord.setEnhancements(List.of(enhancementRecord));
        GenerationEntity generationEntity = generationMapper.toEntity(generationRecord);
        assertThat(generationEntity.getId()).isEqualTo(generationId);
        assertThat(generationEntity.getRequest()).isNotNull();
        assertThat(generationEntity.getRequest().getId()).isEqualTo(requestId);
        assertThat(generationEntity.getEnhancements()).hasSize(1);
        assertThat(generationEntity.getEnhancements()).element(0).extracting("id").isEqualTo(enhancementId);
        assertThat(generationEntity.getGenerationSbomUrls()).containsExactly("https://url1", "https://url2");
        GenerationRecord generationMapperDto = generationMapper.toDto(generationEntity);
        assertThat(generationMapperDto.getId()).isEqualTo(generationId);
        assertThat(generationMapperDto.getRequestId()).isEqualTo(requestId);
        assertThat(generationMapperDto.getEnhancements()).hasSize(1);
        assertThat(generationMapperDto.getEnhancements()).element(0).extracting("id").isEqualTo(enhancementId);
        assertThat(generationMapperDto.getGenerationSbomUrls()).containsExactly("https://url1", "https://url2");
    }

    @Test
    void testEnhancementRecord() {
        EnhancementRecord enhancementRecord = new EnhancementRecord();
        String enhancementId = UUID.randomUUID().toString();
        enhancementRecord.setId(enhancementId);
        String requestId = UUID.randomUUID().toString();
        enhancementRecord.setRequestId(requestId);
        String generationId = UUID.randomUUID().toString();
        enhancementRecord.setGenerationId(generationId);
        enhancementRecord.setEnhancedSbomUrls(List.of("https://url1", "https://url2"));
        EnhancementEntity enhancementEntity = enhancementMapper.toEntity(enhancementRecord);
        assertThat(enhancementEntity.getId()).isEqualTo(enhancementId);
        assertThat(enhancementEntity.getRequest()).isNotNull();
        assertThat(enhancementEntity.getRequest().getId()).isEqualTo(requestId);
        assertThat(enhancementEntity.getGeneration()).isNotNull();
        assertThat(enhancementEntity.getGeneration().getId()).isEqualTo(generationId);
        assertThat(enhancementEntity.getEnhancedSbomUrls()).containsExactly("https://url1", "https://url2");
        EnhancementRecord enhancementMapperDto = enhancementMapper.toDto(enhancementEntity);
        assertThat(enhancementMapperDto.getId()).isEqualTo(enhancementId);
        assertThat(enhancementMapperDto.getRequestId()).isEqualTo(requestId);
        assertThat(enhancementMapperDto.getGenerationId()).isEqualTo(generationId);
        assertThat(enhancementMapperDto.getEnhancedSbomUrls()).containsExactly("https://url1", "https://url2");
    }
}
