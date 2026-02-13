package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.mapper;

import java.util.Optional;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.GenerationEntity;
import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.RequestEntity;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IdMapping {

    // --- Request Mapping ---
    public String mapEntityToId(RequestEntity entity) {
        return Optional.ofNullable(entity).map(RequestEntity::getRequestId).orElse(null);
    }

    public RequestEntity mapRequestId(String requestId) {
        return Optional.ofNullable(requestId).map(id -> {
            RequestEntity entity = new RequestEntity();
            entity.setRequestId(id); // Changed from setId
            return entity;
        }).orElse(null);
    }

    // --- Generation Mapping ---
    public String mapGenerationEntity(GenerationEntity entity) {
        return Optional.ofNullable(entity).map(GenerationEntity::getGenerationId).orElse(null);
    }

    public GenerationEntity mapGenerationId(String generationId) {
        return Optional.ofNullable(generationId).map(id -> {
            GenerationEntity entity = new GenerationEntity();
            entity.setGenerationId(id); // Changed from setId
            return entity;
        }).orElse(null);
    }
}
