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
    // --- SURROGATE KEY ---
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "db_id")
    private Long dbId;

    // --- BUSINESS KEY ---
    @Column(name = "enhancement_id", unique = true, nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String enhancementId;

    private String enhancerName;

    private String enhancerVersion;

    @ElementCollection
    @CollectionTable(name = "enhancement_options", joinColumns = @JoinColumn(name = "enhancement_db_id"))
    @MapKeyColumn(name = "opt_key")
    @Column(name = "opt_value")
    private Map<String, String> enhancerOptions = new HashMap<>();

    @Column(name = "index_value")
    private int index;

    private Instant created;

    private Instant updated;

    private Instant finished;

    @Enumerated(EnumType.STRING)
    private EnhancementStatus status;

    private Integer result;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_db_id")
    private RequestEntity request;

    @ElementCollection
    @CollectionTable(name = "enhancement_sbom_urls", joinColumns = @JoinColumn(name = "enhancement_db_id"))
    @Column(name = "url")
    private Set<String> enhancedSbomUrls = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_db_id")
    private GenerationEntity generation;

}
