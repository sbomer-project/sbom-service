package org.jboss.sbomer.sbom.service.core.domain.dto;

import java.time.Instant;

import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.RunState;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class EnhancementRunRecord {
    @EqualsAndHashCode.Include
    private String id;
    private String enhancementId;
    private Integer attemptNumber;
    private RunState state;
    private EnhancementResult reason;
    private String message;
    private Instant startTime;
    private Instant completionTime;
}

