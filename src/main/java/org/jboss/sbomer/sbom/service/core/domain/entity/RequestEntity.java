package org.jboss.sbomer.sbom.service.core.domain.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.jboss.sbomer.sbom.service.core.domain.enums.RequestStatus;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "requests")
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class RequestEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<GenerationEntity> generations = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "request_publishers", joinColumns = @JoinColumn(name = "request_id"))
    private Set<PublisherEmbeddable> publishers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private Instant creationDate;

    @Embeddable
    @Data
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    public static class PublisherEmbeddable {
        @EqualsAndHashCode.Include
        private String name;

        @EqualsAndHashCode.Include
        private String version;
    }
}
