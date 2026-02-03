package org.jboss.sbomer.sbom.service.adapter.out;

import java.util.ArrayList;
import java.util.List;

import org.jboss.sbomer.events.common.EnhancerSpec;
import org.jboss.sbomer.events.common.GeneratorSpec;
import org.jboss.sbomer.events.orchestration.Recipe;
import org.jboss.sbomer.sbom.service.core.port.spi.RecipeBuilder;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

// This is a mock recipe builder to use temporarily until we add the config provider portion of the code
@ApplicationScoped
@Slf4j
public class MockRecipeBuilder implements RecipeBuilder {
    @Override
    public Recipe buildRecipeFor(String type, String identifier) {
        GeneratorSpec generator;
        List<EnhancerSpec> enhancers = new ArrayList<>();

        // Example logic: choose a different generator based on the target type
        if ("RPM".equals(type)) {
            generator = GeneratorSpec.newBuilder()
                    .setName("cyclonedx-maven-plugin")
                    .setVersion("2.7.9")
                    .build();
            // Maybe RPMs get a special enhancer
            enhancers.add(EnhancerSpec.newBuilder().setName("rpm-enhancer").setVersion("1.0.0").build());

        } else if ("CONTAINER_IMAGE".equals(type)) {
            generator = GeneratorSpec.newBuilder()
                    .setName("syft-generator")
                    .setVersion("1.5.0")
                    .build();
            // enhancers.add(EnhancerSpec.newBuilder().setName("sorting-enhancer").setVersion("1.0.0").build());
        } else {
            // Default or throw an error for unsupported types
            throw new IllegalArgumentException("Unsupported target type: " + type);
        }

        return Recipe.newBuilder()
                .setGenerator(generator)
                .setEnhancers(enhancers)
                .build();
    }
}
