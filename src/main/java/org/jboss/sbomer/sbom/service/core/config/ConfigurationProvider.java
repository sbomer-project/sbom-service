package org.jboss.sbomer.sbom.service.core.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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

    private static final List<String> supportedTypes = List.of("CONTAINER_IMAGE", "RPM");

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
        InputStream is = getClass().getClassLoader()
            .getResourceAsStream(configPath);
        if (is == null) {
            throw new FileNotFoundException("Config file not found: " + configPath);
        }
        config = mapper.readValue(is, SbomerConfig.class);
    }

    private void validateConfiguration() {
        List<String> configuredTypes = config.getRecipes().stream()
            .map(RecipeConfig::getType)
            .toList();

        for (String required : supportedTypes) {
            if (!configuredTypes.contains(required)) {
                throw new IllegalStateException(
                    "Required target type not configured: " + required);
            }
        }
    }

    public RecipeConfig getRecipeForTargetType(String type) {
        return config.getRecipeForType(type);
    }
}

