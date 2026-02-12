package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.PublisherEntity;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.RequestEntity;
import org.jboss.sbomer.sbom.service.core.domain.dto.PublisherRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta-cdi", uses = {GenerationMapper.class, EnhancementMapper.class})
public interface StatusMapper {

    // --- Main Request Mapping ---
    @Mapping(target = "id", source = "requestId") // Entity.requestId -> DTO.id
    @Mapping(target = "publisherRecords", source = "publishers")
    @Mapping(target = "generationRecords", source = "generations")
    RequestRecord toDto(RequestEntity entity);

    @Mapping(target = "requestId", source = "id") // DTO.id -> Entity.requestId
    @Mapping(target = "dbId", ignore = true)      // Ignore DB ID
    @Mapping(target = "publishers", source = "publisherRecords")
    @Mapping(target = "generations", source = "generationRecords")
    RequestEntity toEntity(RequestRecord dto);

    // --- Publisher Mapping ---
    // Publisher doesn't use a business ID in DTO, so we just map fields
    PublisherRecord toPublisherRecord(PublisherEntity entity);

    @Mapping(target = "dbId", ignore = true) // Ignore DB ID
    PublisherEntity toPublisherEntity(PublisherRecord dto);
}
