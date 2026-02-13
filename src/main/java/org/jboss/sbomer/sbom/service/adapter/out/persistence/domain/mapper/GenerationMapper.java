package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.GenerationEntity;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta-cdi", uses = {EnhancementMapper.class, IdMapping.class})
public interface GenerationMapper {

    @Mapping(target = "id", source = "generationId") // Entity.generationId -> DTO.id
    @Mapping(target = "enhancements", source = "enhancements")
    @Mapping(target = "requestId", source = "request") // Uses IdMapping
    GenerationRecord toDto(GenerationEntity entity);

    @Mapping(target = "generationId", source = "id") // DTO.id -> Entity.generationId
    @Mapping(target = "dbId", ignore = true)         // Ignore DB ID
    @Mapping(target = "request", source = "requestId") // Uses IdMapping
    @Mapping(target = "enhancements", source = "enhancements")
    GenerationEntity toEntity(GenerationRecord dto);
}
