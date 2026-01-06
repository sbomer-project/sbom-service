package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper;

import java.util.List;
import java.util.Optional;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.EnhancementEntity;
import org.jboss.sbomer.sbom.service.core.domain.dto.EnhancementRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi", uses = IdMapping.class)
public interface EnhancementMapper {
    @Mapping(target = "generationId", source = "generation")
    @Mapping(target = "requestId", source = "request")
    EnhancementRecord toDto(EnhancementEntity entity);

    @Mapping(target = "generation", source = "generationId")
    @Mapping(target = "request", source = "requestId")
    EnhancementEntity toEntity(EnhancementRecord record);

    default List<EnhancementRecord> map(List<EnhancementEntity> entities) {
        return Optional.ofNullable(entities).map(enhancementEntities -> enhancementEntities.stream().map(this::toDto).toList()).orElse(null);
    }

    default List<EnhancementEntity> mapEnhancements(List<EnhancementRecord> records) {
        return Optional.ofNullable(records).map(enhancementRecords -> enhancementRecords.stream().map(this::toEntity).toList()).orElse(null);
    }
}
