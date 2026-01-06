package org.jboss.sbomer.sbom.service.adapter.out.persistence;

import org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity.EnhancementEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EnhancementRepository implements PanacheRepositoryBase<EnhancementEntity, String> {}
