package org.jboss.sbomer.sbom.service.core.config.recipe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratorConfig {
    private String name;
    private String version;
}
