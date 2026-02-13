package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.jboss.sbomer.sbom.service.core.domain.enums.RequestStatus;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
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
    // --- SURROGATE KEY (Private DB ID) ---
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "db_id")
    private Long dbId;

    // --- BUSINESS KEY (Public TSID) ---
    @Column(name = "request_id", unique = true, nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String requestId;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<GenerationEntity> generations = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_db_id") // This puts the foreign key in the publisher table
    private Set<PublisherEntity> publishers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private Instant creationDate;

}
