package org.jboss.sbomer.sbom.service.adapter.out.persistence;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.EnhancementRunEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EnhancementRunRepository implements PanacheRepositoryBase<EnhancementRunEntity, String> {}

