package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "generations")
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class GenerationEntity extends PanacheEntityBase {
    // --- SURROGATE KEY ---
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "db_id")
    private Long dbId;

    // --- BUSINESS KEY ---
    @Column(name = "generation_id", unique = true, nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String generationId;

    private String generatorName;

    private String generatorVersion;

    @ElementCollection
    @CollectionTable(name = "generation_options", joinColumns = @JoinColumn(name = "generation_db_id"))
    @MapKeyColumn(name = "opt_key")
    @Column(name = "opt_value")
    private Map<String, String> generatorOptions = new HashMap<>();

    private Instant created;

    private Instant updated;

    private Instant finished;

    @Enumerated(EnumType.STRING)
    private GenerationStatus status;

    private Integer result;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_db_id")
    private RequestEntity request;

    private String targetType;

    private String targetIdentifier;

    @ElementCollection
    @CollectionTable(name = "generation_sbom_urls", joinColumns = @JoinColumn(name = "generation_db_id"))
    @Column(name = "url")
    private Set<String> generationSbomUrls = new HashSet<>();

    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<EnhancementEntity> enhancements = new HashSet<>();

    public void setEnhancements(Set<EnhancementEntity> enhancements) {
        this.enhancements = enhancements != null ? new HashSet<>(enhancements) : new HashSet<>();
    }

}
