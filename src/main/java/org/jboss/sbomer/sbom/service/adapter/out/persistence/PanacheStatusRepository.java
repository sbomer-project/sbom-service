package org.jboss.sbomer.sbom.service.adapter.out.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.sbomer.sbom.service.adapter.in.rest.model.Page;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.EnhancementEntity;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.GenerationEntity;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.RequestEntity;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper.EnhancementMapper;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper.GenerationMapper;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper.StatusMapper;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;
import org.jboss.sbomer.sbom.service.core.port.spi.StatusRepository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@ApplicationScoped
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
        // FIXME: Should use persist instead of merge to assure no accidental overwrites
        requestEntity = requestRepository.getEntityManager().merge(requestEntity);
        record.setId(requestEntity.getId());
        Optional.ofNullable(record.getGenerationRecords())
                .ifPresent(generationRecords -> generationRecords.forEach(this::saveGeneration));
    }

    @Override
    @Transactional
    public void updateRequestRecord(RequestRecord record) {
        requestRepository.findByIdOptional(record.getId()).ifPresent(entity -> {
            entity.setStatus(record.getStatus());
        });
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
        // FIXME: Should use persist instead of merge to assure no accidental overwrites
        generationEntity = generationRepository.getEntityManager().merge(generationEntity);
        record.setId(generationEntity.getId());
        Optional.ofNullable(record.getEnhancements())
                .ifPresent(enhancementRecords -> enhancementRecords.forEach(this::saveEnhancement));
    }

    @Override
    public Page<GenerationRecord> findAllGenerations(int pageIndex, int pageSize) {
        PanacheQuery<GenerationEntity> generationEntityPanacheQuery = generationRepository.findAll(Sort.by("id"));
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
        PanacheQuery<GenerationEntity> generationEntityPanacheQuery = generationRepository.find("request.id = ?1",
                Sort.by("id"), requestId);
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

    private void mergeEnhancements(GenerationEntity generationEntity,
            Collection<EnhancementRecord> enhancementRecords) {
        Map<String, EnhancementEntity> existingById = Optional.ofNullable(generationEntity.getEnhancements())
                .orElse(Set.of())
                .stream()
                .filter(e -> e.getId() != null)
                .collect(Collectors.toMap(EnhancementEntity::getId, Function.identity()));
        List<EnhancementEntity> merged = new ArrayList<>();

        if (enhancementRecords != null) {
            for (EnhancementRecord enhancementRecord : enhancementRecords) {
                EnhancementEntity enhancementEntity;

                if (enhancementRecord.getId() != null) {
                    enhancementEntity = existingById.get(enhancementRecord.getId());

                    if (enhancementEntity == null) {
                        enhancementEntity = enhancementRepository.findById(enhancementRecord.getId());

                        if (enhancementEntity == null) {
                            throw new EntityNotFoundException("Unknown enhancement ID " + enhancementRecord.getId());
                        }
                    }
                } else {
                    enhancementEntity = enhancementMapper.toEntity(enhancementRecord);
                    enhancementEntity.setGeneration(generationEntity);
                    enhancementRepository.persist(enhancementEntity);
                    enhancementRecord.setId(enhancementEntity.getId());
                    enhancementRecord.setGenerationId(generationEntity.getId());
                }

                enhancementDtoToEntity(enhancementRecord, enhancementEntity);

                if (enhancementRecord.getRequestId() != null) {
                    RequestEntity req = requestRepository.findById(enhancementRecord.getRequestId());
                    enhancementEntity.setRequest(req);
                } else if (generationEntity.getRequest() != null) {
                    enhancementEntity.setRequest(generationEntity.getRequest()); // XXX
                } else {
                    throw new EntityNotFoundException("Request ID is null");
                }

                merged.add(enhancementEntity);
            }
        }

        if (generationEntity.getEnhancements() != null) {
            generationEntity.getEnhancements().clear();
            generationEntity.getEnhancements().addAll(merged);
        } else {
            generationEntity.setEnhancements(null);
        }
    }

    private static void enhancementDtoToEntity(EnhancementRecord enhancementRecord,
            EnhancementEntity enhancementEntity) {
        enhancementEntity.setEnhancerName(enhancementRecord.getEnhancerName());
        enhancementEntity.setEnhancerVersion(enhancementRecord.getEnhancerVersion());
        enhancementEntity.setIndex(enhancementRecord.getIndex());
        enhancementEntity.setCreated(enhancementRecord.getCreated());
        enhancementEntity.setUpdated(enhancementRecord.getUpdated());
        enhancementEntity.setFinished(enhancementRecord.getFinished());
        enhancementEntity.setStatus(enhancementRecord.getStatus());
        enhancementEntity.setResult(enhancementRecord.getResult());
        enhancementEntity.setReason(enhancementRecord.getReason());

        if (enhancementRecord.getEnhancedSbomUrls() != null) {
            enhancementEntity.getEnhancedSbomUrls().clear();
            enhancementEntity.getEnhancedSbomUrls().addAll(enhancementRecord.getEnhancedSbomUrls());
        } else {
            enhancementEntity.setEnhancedSbomUrls(null);
        }
    }

    @Override
    @Transactional
    public void updateGeneration(GenerationRecord record) {
        generationRepository.findByIdOptional(record.getId()).ifPresent(entity -> {
            entity.setGeneratorName(record.getGeneratorName());
            entity.setGeneratorVersion(record.getGeneratorVersion());
            entity.setCreated(record.getCreated());
            entity.setUpdated(record.getUpdated());
            entity.setFinished(record.getFinished());
            entity.setStatus(record.getStatus());
            entity.setResult(record.getResult());
            entity.setReason(record.getReason());
            entity.setTargetType(record.getTargetType());
            entity.setTargetIdentifier(record.getTargetIdentifier());
            entity.setRequest(record.getRequestId() != null ? requestRepository.findById(record.getRequestId()) : null);

            if (record.getGenerationSbomUrls() != null) {
                entity.getGenerationSbomUrls().clear();
                entity.getGenerationSbomUrls().addAll(record.getGenerationSbomUrls());
            } else {
                entity.setGenerationSbomUrls(null);
            }

            mergeEnhancements(entity, record.getEnhancements());
        });
    }

    // --- ENHANCEMENTS ---

    @Override
    @Transactional
    public void saveEnhancement(EnhancementRecord record) {
        EnhancementEntity enhancementEntity = enhancementMapper.toEntity(record);
        // FIXME: Should use persist instead of merge to assure no accidental overwrites
        enhancementEntity = enhancementRepository.getEntityManager().merge(enhancementEntity);
        record.setId(enhancementEntity.getId());
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
        enhancementRepository.findByIdOptional(record.getId())
                .ifPresent(enhancementEntity -> enhancementDtoToEntity(record, enhancementEntity));
    }

    // --- LOGIC HELPERS ---

    @Override
    public boolean isGenerationAndEnhancementsFinished(String generationId) {
        return generationRepository.findByIdOptional(generationId)
                .filter(generationEntity -> generationEntity.getStatus() == GenerationStatus.FINISHED)
                .map(generationEntity -> {
                    List<EnhancementEntity> children = enhancementRepository.list("generation.id", generationId);
                    return children.isEmpty()
                            || children.stream().allMatch(e -> e.getStatus() == EnhancementStatus.FINISHED);
                })
                .orElse(false);
    }

    @Override
    public boolean isAllGenerationRequestsFinished(String requestId) {
        List<GenerationEntity> generationEntities = generationRepository.list("request.id", requestId);
        return !generationEntities.isEmpty() && generationEntities.stream()
                .allMatch(generationEntity -> isGenerationAndEnhancementsFinished(generationEntity.getId()));
    }

    @Override
    public List<String> getFinalSbomUrlsForCompletedGeneration(String generationId) {
        return List.copyOf(generationRepository.findByIdOptional(generationId)
                .map(generationEntity -> {
                    List<EnhancementEntity> children = enhancementRepository.list("generation.id", generationId);
                    return !children.isEmpty() ? children.stream()
                            .filter(e -> e.getStatus() == EnhancementStatus.FINISHED)
                            .max(Comparator.comparingInt(EnhancementEntity::getIndex))
                            .map(EnhancementEntity::getEnhancedSbomUrls)
                            .orElse(generationEntity.getGenerationSbomUrls())
                            : generationEntity.getGenerationSbomUrls();

                })
                .orElseGet(Set::of));
    }

    @Override
    public List<EnhancementRecord> findEnhancementsByGenerationId(String generationId) {
        List<EnhancementEntity> enhancementEntities = enhancementRepository.list("generation.id", generationId);
        return enhancementEntities.stream()
                .map(enhancementMapper::toDto)
                .toList();
    }

    @Override
    public Page<EnhancementRecord> findAllEnhancements(int pageIndex, int pageSize) {
        PanacheQuery<EnhancementEntity> enhancementEntityPanacheQuery = enhancementRepository.findAll(Sort.by("id"));
        enhancementEntityPanacheQuery.page(pageIndex, pageSize);
        List<EnhancementEntity> enhancementEntities = enhancementEntityPanacheQuery.list();
        long totalHits = enhancementEntityPanacheQuery.count();
        int totalPages = (int) Math.ceil((double) totalHits / pageSize);
        List<EnhancementRecord> enhancementRecords = enhancementEntities.stream()
                .map(enhancementMapper::toDto)
                .toList();
        return Page.<EnhancementRecord>builder()
                .content(enhancementRecords)
                .totalHits(totalHits)
                .totalPages(totalPages)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
    }
}
