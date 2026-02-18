package org.jboss.sbomer.sbom.service.adapter.out.recipe;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.sbomer.events.common.EnhancerSpec;
import org.jboss.sbomer.events.common.GeneratorSpec;
import org.jboss.sbomer.events.orchestration.Recipe;
import org.jboss.sbomer.sbom.service.adapter.out.recipe.config.EnhancerConfig;
import org.jboss.sbomer.sbom.service.adapter.out.recipe.config.GeneratorConfig;
import org.jboss.sbomer.sbom.service.adapter.out.recipe.config.RecipeConfig;
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

        RecipeConfig recipeConfig = configProvider.getRecipeForTargetType(type);
        GeneratorSpec generator = buildGenerator(recipeConfig.getGenerator());
        List<EnhancerSpec> enhancers = buildEnhancers(recipeConfig.getEnhancers());

        log.debug("Built recipe with generator: {}, enhancers: {}", generator.getName(), enhancers);

        return Recipe.newBuilder()
                .setGenerator(generator)
                .setEnhancers(enhancers)
                .build();
    }

    private GeneratorSpec buildGenerator(GeneratorConfig config) {
        GeneratorSpec.Builder builder = GeneratorSpec.newBuilder()
                .setName(config.getName())
                .setVersion(config.getVersion());

        if (config.getOptions() != null) {
            builder.setOptions(config.getOptions());
        }

        return builder.build();
    }

    private List<EnhancerSpec> buildEnhancers(List<EnhancerConfig> configs) {
        if (configs == null) {
            return Collections.emptyList();
        }

        return configs.stream()
                .map(this::buildEnhancer)
                .collect(Collectors.toList());
    }

    private EnhancerSpec buildEnhancer(EnhancerConfig config) {
        EnhancerSpec.Builder builder = EnhancerSpec.newBuilder()
                .setName(config.getName())
                .setVersion(config.getVersion());

        if (config.getOptions() != null) {
            builder.setOptions(config.getOptions());
        }

        return builder.build();
    }
}
