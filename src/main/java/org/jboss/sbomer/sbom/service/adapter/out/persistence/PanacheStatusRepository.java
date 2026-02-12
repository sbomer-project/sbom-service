package org.jboss.sbomer.sbom.service.adapter.out.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
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
import jakarta.persistence.EntityExistsException;
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

    // --- REQUESTS ---

    @Override
    @Transactional
    public void saveRequestRecord(RequestRecord record) {
        if (requestRepository.find("requestId", record.getId()).count() > 0) {
            throw new EntityExistsException("Request with ID " + record.getId() + " already exists.");
        }

        RequestEntity requestEntity = mapper.toEntity(record);

        // Stitch the graph so children point to THIS instance, not a stub
        if (requestEntity.getGenerations() != null) {
            requestEntity.getGenerations().forEach(gen -> {
                gen.setRequest(requestEntity); // Link Generation -> Request

                // Deep Link: Enhancements need to point to this Generation AND Request
                if (gen.getEnhancements() != null) {
                    gen.getEnhancements().forEach(enh -> {
                        enh.setGeneration(gen);
                        enh.setRequest(requestEntity);
                    });
                }
            });
        }

        // Persist the root. CascadeType.ALL handles the rest.
        requestRepository.persist(requestEntity);
    }

    @Override
    @Transactional
    public void updateRequestRecord(RequestRecord record) {
        requestRepository.find("requestId", record.getId()).firstResultOptional().ifPresent(entity -> {
            entity.setStatus(record.getStatus());
        });
    }

    @Override
    public RequestRecord findRequestById(String requestId) {
        return requestRepository.find("requestId", requestId)
            .firstResultOptional()
            .map(mapper::toDto)
            .orElse(null);
    }

    @Override
    public Page<RequestRecord> findAllRequests(int pageIndex, int pageSize) {
        PanacheQuery<RequestEntity> query = requestRepository.findAll(Sort.by("dbId"));
        query.page(pageIndex, pageSize);
        List<RequestRecord> records = query.list().stream().map(mapper::toDto).toList();
        return Page.<RequestRecord>builder()
            .content(records)
            .totalHits(query.count())
            .totalPages(query.pageCount())
            .pageIndex(pageIndex)
            .pageSize(pageSize)
            .build();
    }

    // --- GENERATIONS ---

    @Override
    @Transactional
    public void saveGeneration(GenerationRecord record) {
        if (generationRepository.find("generationId", record.getId()).count() > 0) {
            return;
        }

        GenerationEntity generationEntity = generationMapper.toEntity(record);

        // 1. Fetch Real Parent
        if (record.getRequestId() != null) {
            RequestEntity parent = requestRepository.find("requestId", record.getRequestId()).firstResult();
            generationEntity.setRequest(parent);
        }

        // 2. Stitch Children (Enhancements)
        if (generationEntity.getEnhancements() != null) {
            generationEntity.getEnhancements().forEach(enh -> {
                enh.setGeneration(generationEntity); // Replace Stub with Real Entity

                // If the generation knows its request, pass it down
                if (generationEntity.getRequest() != null) {
                    enh.setRequest(generationEntity.getRequest());
                }
            });
        }

        // 3. Persist Root (Cascade handles children)
        generationRepository.persist(generationEntity);
    }

    @Override
    public Page<GenerationRecord> findAllGenerations(int pageIndex, int pageSize) {
        PanacheQuery<GenerationEntity> query = generationRepository.findAll(Sort.by("dbId"));
        query.page(pageIndex, pageSize);
        List<GenerationRecord> records = query.list().stream().map(generationMapper::toDto).toList();

        return Page.<GenerationRecord>builder()
            .content(records)
            .totalHits(query.count())
            .totalPages(query.pageCount())
            .pageIndex(pageIndex)
            .pageSize(pageSize)
            .build();
    }

    @Override
    public GenerationRecord findGenerationById(String generationId) {
        return generationRepository.find("generationId", generationId)
            .firstResultOptional()
            .map(generationMapper::toDto)
            .orElse(null);
    }

    @Override
    public List<GenerationRecord> findGenerationsByRequestId(String requestId) {
        return generationRepository.list("request.requestId", requestId).stream()
            .map(generationMapper::toDto)
            .toList();
    }

    @Override
    public Page<GenerationRecord> findGenerationsByRequestId(String requestId, int pageIndex, int pageSize) {
        PanacheQuery<GenerationEntity> query = generationRepository.find("request.requestId",
            Sort.by("dbId"), requestId);
        query.page(pageIndex, pageSize);
        List<GenerationRecord> records = query.list().stream().map(generationMapper::toDto).toList();

        return Page.<GenerationRecord>builder()
            .content(records)
            .totalHits(query.count())
            .totalPages(query.pageCount())
            .pageIndex(pageIndex)
            .pageSize(pageSize)
            .build();
    }

    @Override
    public List<GenerationRecord> findByGenerationStatus(GenerationStatus status) {
        return generationRepository.list("status", status).stream()
            .map(generationMapper::toDto)
            .toList();
    }

    @Override
    @Transactional
    public void updateGeneration(GenerationRecord record) {
        generationRepository.find("generationId", record.getId()).firstResultOptional().ifPresent(entity -> {
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

            if (record.getRequestId() != null) {
                RequestEntity req = requestRepository.find("requestId", record.getRequestId()).firstResult();
                entity.setRequest(req);
            }

            if (record.getGeneratorOptions() != null) {
                if (entity.getGeneratorOptions() == null) {
                    entity.setGeneratorOptions(new HashMap<>());
                }
                entity.getGeneratorOptions().clear();
                entity.getGeneratorOptions().putAll(record.getGeneratorOptions());
            } else {
                entity.setGeneratorOptions(null);
            }

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
        if (enhancementRepository.find("enhancementId", record.getId()).count() > 0) {
            return;
        }

        EnhancementEntity entity = enhancementMapper.toEntity(record);

        if (record.getRequestId() != null) {
            RequestEntity req = requestRepository.find("requestId", record.getRequestId()).firstResult();
            entity.setRequest(req);
        }
        if (record.getGenerationId() != null) {
            GenerationEntity gen = generationRepository.find("generationId", record.getGenerationId()).firstResult();
            entity.setGeneration(gen);
        }

        enhancementRepository.persist(entity);
    }

    @Override
    public EnhancementRecord findEnhancementById(String enhancementId) {
        return enhancementRepository.find("enhancementId", enhancementId)
            .firstResultOptional()
            .map(enhancementMapper::toDto)
            .orElse(null);
    }

    @Override
    public List<EnhancementRecord> findByEnhancementStatus(EnhancementStatus status) {
        return enhancementRepository.list("status", status).stream()
            .map(enhancementMapper::toDto)
            .toList();
    }

    @Override
    @Transactional
    public void updateEnhancement(EnhancementRecord record) {
        enhancementRepository.find("enhancementId", record.getId())
            .firstResultOptional()
            .ifPresent(enhancementEntity -> enhancementDtoToEntity(record, enhancementEntity));
    }

    // --- LOGIC HELPERS ---

    @Override
    public boolean isGenerationAndEnhancementsFinished(String generationId) {
        return generationRepository.find("generationId", generationId).firstResultOptional()
            .filter(generationEntity -> generationEntity.getStatus() == GenerationStatus.FINISHED)
            .map(generationEntity -> {
                List<EnhancementEntity> children = enhancementRepository.list("generation.generationId", generationId);
                return children.isEmpty()
                    || children.stream().allMatch(e -> e.getStatus() == EnhancementStatus.FINISHED);
            })
            .orElse(false);
    }

    @Override
    public boolean isAllGenerationRequestsFinished(String requestId) {
        List<GenerationEntity> generationEntities = generationRepository.list("request.requestId", requestId);
        return !generationEntities.isEmpty() && generationEntities.stream()
            .allMatch(generationEntity -> isGenerationAndEnhancementsFinished(generationEntity.getGenerationId()));
    }

    @Override
    public List<String> getFinalSbomUrlsForCompletedGeneration(String generationId) {
        return List.copyOf(generationRepository.find("generationId", generationId).firstResultOptional()
            .map(generationEntity -> {
                List<EnhancementEntity> children = enhancementRepository.list("generation.generationId", generationId);
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
        return enhancementRepository.list("generation.generationId", generationId).stream()
            .map(enhancementMapper::toDto)
            .toList();
    }

    @Override
    public Page<EnhancementRecord> findAllEnhancements(int pageIndex, int pageSize) {
        PanacheQuery<EnhancementEntity> query = enhancementRepository.findAll(Sort.by("dbId"));
        query.page(pageIndex, pageSize);
        List<EnhancementRecord> records = query.list().stream().map(enhancementMapper::toDto).toList();

        return Page.<EnhancementRecord>builder()
            .content(records)
            .totalHits(query.count())
            .totalPages(query.pageCount())
            .pageIndex(pageIndex)
            .pageSize(pageSize)
            .build();
    }

    // --- PRIVATE HELPERS ---

    private void mergeEnhancements(GenerationEntity generationEntity,
                                   Collection<EnhancementRecord> enhancementRecords) {
        Map<String, EnhancementEntity> existingById = Optional.ofNullable(generationEntity.getEnhancements())
            .orElse(Set.of())
            .stream()
            .filter(e -> e.getEnhancementId() != null)
            .collect(Collectors.toMap(EnhancementEntity::getEnhancementId, Function.identity()));

        List<EnhancementEntity> merged = new ArrayList<>();

        if (enhancementRecords != null) {
            for (EnhancementRecord enhancementRecord : enhancementRecords) {
                EnhancementEntity enhancementEntity;

                if (enhancementRecord.getId() != null && existingById.containsKey(enhancementRecord.getId())) {
                    enhancementEntity = existingById.get(enhancementRecord.getId());
                } else {
                    EnhancementEntity dbEntity = enhancementRepository.find("enhancementId", enhancementRecord.getId()).firstResult();
                    if (dbEntity != null) {
                        enhancementEntity = dbEntity;
                    } else {
                        enhancementEntity = enhancementMapper.toEntity(enhancementRecord);

                        // FIX: Ensure new entity points to THIS generation, not a stub
                        enhancementEntity.setGeneration(generationEntity);

                        // Ensure request is linked
                        if (enhancementRecord.getRequestId() != null) {
                            RequestEntity req = requestRepository.find("requestId", enhancementRecord.getRequestId()).firstResult();
                            enhancementEntity.setRequest(req);
                        } else if (generationEntity.getRequest() != null) {
                            enhancementEntity.setRequest(generationEntity.getRequest());
                        }

                        // We do NOT need to persist explicitly if we add it to the parent's collection
                        // and merge the parent. But if we want the ID immediately, we persist.
                        enhancementRepository.persist(enhancementEntity);
                    }
                }

                enhancementDtoToEntity(enhancementRecord, enhancementEntity);
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

        if (enhancementRecord.getEnhancerOptions() != null) {
            if (enhancementEntity.getEnhancerOptions() == null) {
                enhancementEntity.setEnhancerOptions(new HashMap<>());
            }
            enhancementEntity.getEnhancerOptions().clear();
            enhancementEntity.getEnhancerOptions().putAll(enhancementRecord.getEnhancerOptions());
        } else {
            enhancementEntity.setEnhancerOptions(null);
        }

        if (enhancementRecord.getEnhancedSbomUrls() != null) {
            enhancementEntity.getEnhancedSbomUrls().clear();
            enhancementEntity.getEnhancedSbomUrls().addAll(enhancementRecord.getEnhancedSbomUrls());
        } else {
            enhancementEntity.setEnhancedSbomUrls(null);
        }
    }
}
