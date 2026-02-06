package org.jboss.sbomer.sbom.service.core.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.sbomer.sbom.service.core.config.recipe.RecipeConfig;
import org.jboss.sbomer.sbom.service.core.config.recipe.SbomerConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ConfigurationProvider {
    private SbomerConfig config;

    @ConfigProperty(name = "sbomer.config.path", defaultValue = "sbomer-config.yaml")
    String configPath;

    @PostConstruct
    void init() {
        try {
            loadConfiguration();
            validateConfiguration();
            log.info("Loaded recipe configuration for {} target types",
                config.getRecipes().size());
        } catch (Exception e) {
            log.error("Failed to load configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }
    }

    private void loadConfiguration() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        InputStream inputStream = getClass().getClassLoader()
            .getResourceAsStream(configPath);
        if (inputStream == null) {
            throw new FileNotFoundException("Config file not found: " + configPath);
        }
        config = mapper.readValue(inputStream, SbomerConfig.class);
    }

    private void validateConfiguration() {
        if (config.getRecipes() == null || config.getRecipes().isEmpty()) {
            throw new IllegalStateException("No recipes configured in " + configPath);
        }

        // Validate each recipe has required fields
        for (RecipeConfig recipe : config.getRecipes()) {
            if (recipe.getType() == null || recipe.getType().isBlank()) {
                throw new IllegalStateException("Recipe type cannot be null or empty");
            }
            if (recipe.getGenerator() == null) {
                throw new IllegalStateException(
                    "Recipe for type '" + recipe.getType() + "' must have a generator configured");
            }
        }
    }

    public RecipeConfig getRecipeForTargetType(String type) {
        return config.getRecipeForType(type);
    }
}

