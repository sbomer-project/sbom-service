package org.jboss.sbomer.sbom.service.core.domain.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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
import jakarta.persistence.Table;
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
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    private String enhancerName;

    private String enhancerVersion;

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
}
