package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper;

import java.util.List;
import java.util.Optional;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.EnhancementEntity;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta-cdi", uses = IdMapping.class)
public interface EnhancementMapper {

    @Mapping(target = "id", source = "enhancementId") // Entity.enhancementId -> DTO.id
    @Mapping(target = "generationId", source = "generation") // Uses IdMapping
    @Mapping(target = "requestId", source = "request")       // Uses IdMapping
    EnhancementRecord toDto(EnhancementEntity entity);

    @Mapping(target = "enhancementId", source = "id") // DTO.id -> Entity.enhancementId
    @Mapping(target = "dbId", ignore = true)          // Ignore DB ID
    @Mapping(target = "generation", source = "generationId") // Uses IdMapping
    @Mapping(target = "request", source = "requestId")       // Uses IdMapping
    EnhancementEntity toEntity(EnhancementRecord record);

    // List helpers
    default List<EnhancementRecord> map(List<EnhancementEntity> entities) {
        return Optional.ofNullable(entities)
            .map(e -> e.stream().map(this::toDto).toList())
            .orElse(null);
    }

    default List<EnhancementEntity> mapEnhancements(List<EnhancementRecord> records) {
        return Optional.ofNullable(records)
            .map(r -> r.stream().map(this::toEntity).toList())
            .orElse(null);
    }
}
