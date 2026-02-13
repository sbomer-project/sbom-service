package org.jboss.sbomer.sbom.service.core.domain.dto;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementStatus;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO representing the state of a single enhancement task.
 * A GenerationRecord can have one or more of these.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class EnhancementRecord {
    @EqualsAndHashCode.Include
    private String id;
    private String enhancerName;
    private String enhancerVersion;
    /**
     * Configuration options specific to the enhancer (e.g., specific flags, exclusions).
     * Mapped from the configuration recipe.
     */
    private Map<String, String> enhancerOptions;
    /**
     * The 0-based order in which this enhancement step should be executed.
     */
    private int index;
    private Instant created;
    private Instant updated;
    private Instant finished;
    private EnhancementStatus status;
    private Integer result;
    private String reason;
    private String requestId;
    private Collection<String> enhancedSbomUrls;
    private String generationId;
}
