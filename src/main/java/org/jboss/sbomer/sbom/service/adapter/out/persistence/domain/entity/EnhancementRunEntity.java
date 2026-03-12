package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity;

import java.time.Instant;

import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.RunState;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a single execution attempt of an Enhancement in the background worker.
 * This is an append-only log of attempts, providing full audit trail.
 */
@Entity
@Table(name = "enhancement_runs")
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class EnhancementRunEntity extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "db_id")
    private Long dbId;

    @Column(name = "run_id", unique = true, nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String runId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enhancement_db_id", nullable = false)
    private EnhancementEntity enhancement;

    /**
     * The attempt number for this run (1-based).
     * First attempt = 1, first retry = 2, etc.
     */
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    /**
     * Current execution state of this run.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunState state;

    /**
     * The specific result/reason code for this run.
     * Null while running, populated on completion.
     */
    @Enumerated(EnumType.STRING)
    private EnhancementResult reason;

    /**
     * Human-readable error message or context.
     */
    @Column(length = 1000)
    private String message;

    /**
     * When this run started executing.
     */
    @Column(name = "start_time")
    private Instant startTime;

    /**
     * When this run completed (success or failure).
     */
    @Column(name = "completion_time")
    private Instant completionTime;
}
