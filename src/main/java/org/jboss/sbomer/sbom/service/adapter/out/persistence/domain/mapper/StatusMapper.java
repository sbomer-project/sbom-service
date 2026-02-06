package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper;

import java.util.List;
import java.util.Optional;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.RequestEntity;
import org.jboss.sbomer.sbom.service.core.domain.dto.PublisherRecord;
import org.jboss.sbomer.sbom.service.core.domain.dto.RequestRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta-cdi", uses = {GenerationMapper.class, EnhancementMapper.class})
public interface StatusMapper {
    @Mapping(target = "generationRecords", source = "generations")
    @Mapping(target = "publisherRecords", source = "publishers")
    RequestRecord toDto(RequestEntity entity);

    @Mapping(target = "generations", source = "generationRecords")
    @Mapping(target = "publishers", source = "publisherRecords")
    @Mapping(target = "id", source = "id")
    RequestEntity toEntity(RequestRecord dto);

    default List<PublisherRecord> map(List<RequestEntity.PublisherEmbeddable> publishers) {
        return Optional.ofNullable(publishers).map(publisherEmbeddables -> publisherEmbeddables.stream()
            .map(p -> {
                PublisherRecord r = new PublisherRecord();
                r.setName(p.getName());
                r.setVersion(p.getVersion());
                return r;
            }).toList()).orElse(null);

    }

    default List<RequestEntity.PublisherEmbeddable> mapPublishers(List<PublisherRecord> records) {
        return Optional.ofNullable(records).map(publisherRecords -> publisherRecords.stream()
            .map(r -> {
                RequestEntity.PublisherEmbeddable p = new RequestEntity.PublisherEmbeddable();
                p.setName(r.getName());
                p.setVersion(r.getVersion());
                return p;
            }).toList()).orElse(null);
    }
}
