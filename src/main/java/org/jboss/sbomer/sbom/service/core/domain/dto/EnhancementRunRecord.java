package org.jboss.sbomer.sbom.service.core.domain.dto;

import java.time.Instant;

import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;
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
    
    /**
     * Canonical error result code.
     * Null while running or on success, populated on failure.
     */
    private ErrorResult errorResult;
    
    private String message;
    
    /**
     * Raw upstream reason from external worker (e.g., "TaskRunFailed").
     */
    private String upstreamReason;
    
    private Instant startTime;
    private Instant completionTime;
}

