package org.jboss.sbomer.sbom.service.core.domain.dto;

import java.time.Instant;
import java.util.List;

import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationStatus;

import lombok.Getter;
import lombok.Setter;

    @Getter
    @Setter
    public class GenerationRecord {
        private String id;
        private String generatorName;
        private String generatorVersion;
        private Instant created;
        private Instant updated;
        private Instant finished;
        private GenerationStatus status;
        private Integer result;
        private String reason;
        private String requestId;
        private String targetType;
        private String targetIdentifier;
        private List<String> generationSbomUrls;
        private List<EnhancementRecord> enhancements;
    }
