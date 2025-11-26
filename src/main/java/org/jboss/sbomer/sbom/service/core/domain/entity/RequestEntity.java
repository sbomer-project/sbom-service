package org.jboss.sbomer.sbom.service.core.domain.entity;

import java.time.Instant;
import java.util.List;

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
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "requests")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class RequestEntity extends PanacheEntityBase {
    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GenerationEntity> generations;

    @ElementCollection
    @CollectionTable(name = "request_publishers", joinColumns = @JoinColumn(name = "request_id"))
    private List<PublisherEmbeddable> publishers;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private Instant creationDate;

    @Embeddable
    @Data
    public static class PublisherEmbeddable {
        private String name;

        private String version;
    }
}
