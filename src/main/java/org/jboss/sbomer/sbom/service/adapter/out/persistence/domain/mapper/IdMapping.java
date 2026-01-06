package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper;

import java.util.Optional;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.GenerationEntity;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.RequestEntity;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IdMapping {
    public String mapEntityToId(RequestEntity entity) {
        return Optional.ofNullable(entity).map(RequestEntity::getId).orElse(null);
    }

    private static RequestEntity newRequestEntity(final String requestId) {
        RequestEntity entity = new RequestEntity();
        entity.setId(requestId);
        return entity;
    }

    public RequestEntity mapRequestId(String requestId) {
        return Optional.ofNullable(requestId).map(IdMapping::newRequestEntity).orElse(null);

    }

    private static GenerationEntity newGenerationEntity(final String generationId) {
        GenerationEntity entity = new GenerationEntity();
        entity.setId(generationId);
        return entity;
    }

    public GenerationEntity mapGenerationId(String generationId) {
        return Optional.ofNullable(generationId).map(IdMapping::newGenerationEntity).orElse(null);

    }

    public String mapGenerationEntity(GenerationEntity entity) {
        return Optional.ofNullable(entity).map(GenerationEntity::getId).orElse(null);
    }
}
