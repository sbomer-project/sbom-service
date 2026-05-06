package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.GenerationEntity;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi", uses = {EnhancementMapper.class, IdMapping.class})
public interface GenerationMapper {

    @Mapping(target = "id", source = "generationId") // Entity.generationId -> DTO.id
    @Mapping(target = "enhancements", source = "enhancements")
    @Mapping(target = "requestId", source = "request") // Uses IdMapping
    @Mapping(target = "runs", ignore = true)
    GenerationRecord toDto(GenerationEntity entity);

    @Mapping(target = "generationId", source = "id") // DTO.id -> Entity.generationId
    @Mapping(target = "dbId", ignore = true)         // Ignore DB ID
    @Mapping(target = "request", source = "requestId") // Uses IdMapping
    @Mapping(target = "enhancements", source = "enhancements")
    @Mapping(target = "runs", ignore = true)
    GenerationEntity toEntity(GenerationRecord dto);
}
