package org.jboss.sbomer.sbom.service.adapter.out.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jboss.sbomer.sbom.service.adapter.in.rest.model.Page;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.entity.EnhancementEntity;
import org.jboss.sbomer.sbom.service.core.domain.entity.GenerationEntity;
import org.jboss.sbomer.sbom.service.core.domain.entity.RequestEntity;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;
import org.jboss.sbomer.sbom.service.core.mapper.EnhancementMapper;
import org.jboss.sbomer.sbom.service.core.mapper.GenerationMapper;
import org.jboss.sbomer.sbom.service.core.mapper.StatusMapper;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;

import io.quarkus.arc.DefaultBean;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
@DefaultBean
@Transactional
public class PanacheStatusRepository implements StatusRepository {
    @Inject
    RequestRepository requestRepository;

    @Inject
    GenerationRepository generationRepository;

    @Inject
    EnhancementRepository enhancementRepository;

    @Inject
    StatusMapper mapper;

    @Inject
    GenerationMapper generationMapper;

    @Inject
    EnhancementMapper enhancementMapper;

    @Override
    @Transactional
    public void saveRequestRecord(RequestRecord record) {
        RequestEntity requestEntity = mapper.toEntity(record);
        requestRepository.persist(requestEntity);
        Optional.ofNullable(record.getGenerationRecords()).ifPresent(generationRecords -> generationRecords.forEach(this::saveGeneration));
    }

    @Override
    public RequestRecord findRequestById(String requestId) {
        return requestRepository.findByIdOptional(requestId)
                .map(mapper::toDto)
                .orElse(null);
    }

    @Override
    public Page<RequestRecord> findAllRequests(int pageIndex, int pageSize) {
        PanacheQuery<RequestEntity> requestEntityPanacheQuery = requestRepository.findAll(Sort.by("id"));
        requestEntityPanacheQuery.page(pageIndex, pageSize);
        List<RequestEntity> requestEntities = requestEntityPanacheQuery.list();
        long totalHits = requestEntityPanacheQuery.count();
        int totalPages = (int) Math.ceil((double) totalHits / pageSize);
        List<RequestRecord> requestRecords = requestEntities.stream()
                .map(mapper::toDto)
                .toList();
        return Page.<RequestRecord>builder()
                .content(requestRecords)
                .totalHits(totalHits)
                .totalPages(totalPages)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
    }

    // --- GENERATIONS ---

    @Override
    @Transactional
    public void saveGeneration(GenerationRecord record) {
        GenerationEntity generationEntity = generationMapper.toEntity(record);
        generationRepository.persist(generationEntity);
        Optional.ofNullable(record.getEnhancements()).ifPresent(enhancementRecords ->  enhancementRecords.forEach(this::saveEnhancement));
    }

    @Override
    public GenerationRecord findGenerationById(String generationId) {
        return generationRepository.findByIdOptional(generationId)
                .map(generationMapper::toDto)
                .orElse(null);
    }

    @Override
    public List<GenerationRecord> findGenerationsByRequestId(String requestId) {
        List<GenerationEntity> generationEntities = generationRepository.list("request.id", requestId);
        return generationEntities.stream()
                .map(generationMapper::toDto)
                .toList();
    }

    @Override
    public Page<GenerationRecord> findGenerationsByRequestId(String requestId, int pageIndex, int pageSize) {
        PanacheQuery<GenerationEntity> generationEntityPanacheQuery = generationRepository.find("request.id = ?1", Sort.by("id"), requestId);
        generationEntityPanacheQuery.page(pageIndex, pageSize);
        List<GenerationEntity> generationEntities = generationEntityPanacheQuery.list();
        long totalHits = generationEntityPanacheQuery.count();
        int totalPages = (int) Math.ceil((double) totalHits / pageSize);
        List<GenerationRecord> generationRecords = generationEntities.stream()
                .map(generationMapper::toDto)
                .toList();
        return Page.<GenerationRecord>builder()
                .content(generationRecords)
                .totalHits(totalHits)
                .totalPages(totalPages)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public List<GenerationRecord> findByGenerationStatus(GenerationStatus status) {
        List<GenerationEntity> generationEntities = generationRepository.list("status", status);
        return generationEntities.stream()
                .map(generationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void updateGeneration(GenerationRecord record) {
        GenerationEntity generationEntity = generationMapper.toEntity(record);
        generationRepository.persist(generationEntity);
    }

    // --- ENHANCEMENTS ---

    @Override
    @Transactional
    public void saveEnhancement(EnhancementRecord record) {
        EnhancementEntity enhancementEntity = enhancementMapper.toEntity(record);
        enhancementRepository.persist(enhancementEntity);
    }

    @Override
    public EnhancementRecord findEnhancementById(String enhancementId) {
        return enhancementRepository.findByIdOptional(enhancementId)
                .map(enhancementMapper::toDto)
                .orElse(null);
    }

    @Override
    public List<EnhancementRecord> findByEnhancementStatus(EnhancementStatus status) {
        List<EnhancementEntity> enhancementEntities = enhancementRepository.list("status", status);
        return enhancementEntities.stream()
                .map(enhancementMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void updateEnhancement(EnhancementRecord record) {
        EnhancementEntity enhancementEntity = enhancementMapper.toEntity(record);
        enhancementRepository.persist(enhancementEntity);
    }

    // --- LOGIC HELPERS ---

    @Override
    public boolean isGenerationAndEnhancementsFinished(String generationId) {
        return generationRepository.findByIdOptional(generationId)
            .filter(generationEntity -> generationEntity.getStatus() == GenerationStatus.FINISHED)
            .map(generationEntity -> {
                List<EnhancementEntity> children = enhancementRepository.list("generation.id", generationId);
                return children.isEmpty() || children.stream().allMatch(e -> e.getStatus() == EnhancementStatus.FINISHED);
            })
            .orElse(false);
    }

    @Override
    public boolean isAllGenerationRequestsFinished(String requestId) {
        List<GenerationEntity> generationEntities = generationRepository.list("request.id", requestId);
        return !generationEntities.isEmpty() && generationEntities.stream().allMatch(generationEntity -> isGenerationAndEnhancementsFinished(generationEntity.getId()));
    }

    @Override
    public List<String> getFinalSbomUrlsForCompletedGeneration(String generationId) {
        return generationRepository.findByIdOptional(generationId)
            .map(generationEntity -> {
                List<EnhancementEntity> children = enhancementRepository.list("generation.id", generationId);
                return !children.isEmpty() ? children.stream()
                    .filter(e -> e.getStatus() == EnhancementStatus.FINISHED)
                    .max(Comparator.comparingInt(EnhancementEntity::getIndex))
                    .map(EnhancementEntity::getEnhancedSbomUrls)
                    .orElse(generationEntity.getGenerationSbomUrls()) : generationEntity.getGenerationSbomUrls();

            })
            .orElseGet(List::of);
    }
}
