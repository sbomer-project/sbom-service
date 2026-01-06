package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.GenerationEntity;
import org.jboss.sbomer.sbom.service.core.domain.dto.GenerationRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi", uses = {EnhancementMapper.class, IdMapping.class})
public interface GenerationMapper {
    @Mapping(target = "enhancements", source = "enhancements")
    @Mapping(target = "requestId", source = "request")
    GenerationRecord toDto(GenerationEntity entity);

    @Mapping(target = "request", source = "requestId")
    @Mapping(target = "enhancements", source = "enhancements")
    GenerationEntity toEntity(GenerationRecord dto);
}
