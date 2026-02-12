package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "enhancements")
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class EnhancementEntity extends PanacheEntityBase {
    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    private String enhancerName;

    private String enhancerVersion;

    @ElementCollection
    @CollectionTable(name = "enhancement_options", joinColumns = @JoinColumn(name = "enhancement_id"))
    @MapKeyColumn(name = "opt_key")
    @Column(name = "opt_value")
    private Map<String, String> enhancerOptions = new HashMap<>();

    private int index;

    private Instant created;

    private Instant updated;

    private Instant finished;

    @Enumerated(EnumType.STRING)
    private EnhancementStatus status;

    private Integer result;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private RequestEntity request;

    @ElementCollection
    @CollectionTable(name = "enhancement_sbom_urls", joinColumns = @JoinColumn(name = "enhancement_id"))
    @Column(name = "url")
    private Set<String> enhancedSbomUrls = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id")
    private GenerationEntity generation;

    // This acts as the "Default" generator.
    // If we provide an ID (TSID/Test ID), this does nothing.
    // If we provide null, this generates a UUID.
    @PrePersist
    public void ensureId() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }

}
