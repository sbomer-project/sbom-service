package org.jboss.sbomer.sbom.service.core.domain.dto;

import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class PublisherRecord {
    @EqualsAndHashCode.Include
    private String name;
    @EqualsAndHashCode.Include
    private String version;
    private Map<String, String> options;
}
