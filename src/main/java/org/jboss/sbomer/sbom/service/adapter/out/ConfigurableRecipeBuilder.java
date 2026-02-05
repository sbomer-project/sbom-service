package org.jboss.sbomer.sbom.service.adapter.out;

import java.util.ArrayList;
import java.util.List;

import org.jboss.sbomer.events.common.EnhancerSpec;
import org.jboss.sbomer.events.common.GeneratorSpec;
import org.jboss.sbomer.events.orchestration.Recipe;
import org.jboss.sbomer.sbom.service.core.config.ConfigurationProvider;
import org.jboss.sbomer.sbom.service.core.config.recipe.EnhancerConfig;
import org.jboss.sbomer.sbom.service.core.config.recipe.RecipeConfig;
import org.jboss.sbomer.sbom.service.core.port.spi.RecipeBuilder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ConfigurableRecipeBuilder implements RecipeBuilder {

    private final ConfigurationProvider configProvider;

    @Inject
    public ConfigurableRecipeBuilder(ConfigurationProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public Recipe buildRecipeFor(String type, String identifier) {
        log.debug("Building recipe for type: {}, identifier: {}", type, identifier);

        // Get recipe configuration for this target type
        RecipeConfig recipeConfig = configProvider.getRecipeForTargetType(type);

        // Build GeneratorSpec from config
        GeneratorSpec generator = GeneratorSpec.newBuilder()
            .setName(recipeConfig.getGenerator().getName())
            .setVersion(recipeConfig.getGenerator().getVersion())
            .build();

        // Build EnhancerSpecs from config
        List<EnhancerSpec> enhancers = new ArrayList<>();
        if (recipeConfig.getEnhancers() != null) {
            for (EnhancerConfig enhancerConfig : recipeConfig.getEnhancers()) {
                enhancers.add(EnhancerSpec.newBuilder()
                    .setName(enhancerConfig.getName())
                    .setVersion(enhancerConfig.getVersion())
                    .build());
            }
        }

        log.debug("Built recipe with generator: {}, enhancers: {}",
            generator.getName(), enhancers.size());

        return Recipe.newBuilder()
            .setGenerator(generator)
            .setEnhancers(enhancers)
            .build();
    }
}

