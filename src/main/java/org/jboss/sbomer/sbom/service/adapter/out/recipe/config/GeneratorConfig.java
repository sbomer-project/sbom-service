package org.jboss.sbomer.sbom.service.adapter.out.recipe.config;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratorConfig {
    private String name;
    private String version;
    private Map<String, String> options;
}
