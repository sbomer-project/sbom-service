package org.jboss.sbomer.sbom.service.adapter.out.persistence;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.GenerationEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GenerationRepository implements PanacheRepositoryBase<GenerationEntity, String> {}
