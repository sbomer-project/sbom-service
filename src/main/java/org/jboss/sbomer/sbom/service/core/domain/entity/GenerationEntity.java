package org.jboss.sbomer.sbom.service.core.domain.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    private String generatorName;

    private String generatorVersion;

    private Instant created;

    private Instant updated;

    private Instant finished;

    @Enumerated(EnumType.STRING)
    private GenerationStatus status;

    private Integer result;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private RequestEntity request;

    private String targetType;

    private String targetIdentifier;

    @ElementCollection
    @CollectionTable(name = "generation_sbom_urls", joinColumns = @JoinColumn(name = "generation_id"))
    @Column(name = "url")
    private Set<String> generationSbomUrls = new HashSet<>();

    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<EnhancementEntity> enhancements = new HashSet<>();

    public void setEnhancements(Set<EnhancementEntity> enhancements) {
        this.enhancements = enhancements != null ? new HashSet<>(enhancements) : new HashSet<>();
    }
}
