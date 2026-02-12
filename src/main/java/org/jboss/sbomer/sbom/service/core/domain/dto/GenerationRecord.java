package org.jboss.sbomer.sbom.service.core.domain.dto;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class GenerationRecord {
    @EqualsAndHashCode.Include
    private String id;
    private String generatorName;
    private String generatorVersion;
    /**
     * Configuration options specific to the generator (e.g., specific flags, exclusions).
     * Mapped from the configuration recipe.
     */
    private Map<String, String> generatorOptions;
    private Instant created;
    private Instant updated;
    private Instant finished;
    private GenerationStatus status;
    private Integer result;
    private String reason;
    private String requestId;
    private String targetType;
    private String targetIdentifier;
    private Collection<String> generationSbomUrls;
    private Collection<EnhancementRecord> enhancements;
}
