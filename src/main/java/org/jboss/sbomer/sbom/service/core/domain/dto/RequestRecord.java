package org.jboss.sbomer.sbom.service.core.domain.dto;

import java.time.Instant;
import java.util.Collection;

import org.jboss.sbomer.sbom.service.core.domain.enums.RequestStatus;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class RequestRecord {
    @EqualsAndHashCode.Include
    private String id;
    private Collection<GenerationRecord> generationRecords;
    private Collection<PublisherRecord> publisherRecords;
    private RequestStatus status;
    private Instant creationDate;
}
