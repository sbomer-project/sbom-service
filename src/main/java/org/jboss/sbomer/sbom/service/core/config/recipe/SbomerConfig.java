package org.jboss.sbomer.sbom.service.core.config.recipe;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SbomerConfig {
    private String apiVersion;
    private List<RecipeConfig> recipes;

    public RecipeConfig getRecipeForType(String targetType) {
        return recipes.stream().filter(r -> r.getTargetType().equalsIgnoreCase(targetType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported target type: " + targetType));

    }
}
