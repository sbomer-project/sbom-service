package org.jboss.sbomer.sbom.service.core.config.recipe;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeConfig {
    private String type;
    private GeneratorConfig generator;
    private List<EnhancerConfig> enhancers;
}
