package org.jboss.sbomer.test.unit.sbom.service.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.jboss.sbomer.events.orchestration.Recipe;
import org.jboss.sbomer.sbom.service.adapter.out.ConfigurableRecipeBuilder;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ConfigurableRecipeBuilderTest {

    @Inject
    ConfigurableRecipeBuilder recipeBuilder;

    @Test
    void shouldBuildRecipeForContainerImage() {
        // When
        Recipe recipe = recipeBuilder.buildRecipeFor("CONTAINER_IMAGE", "quay.io/example/image:latest");

        // Then
        assertThat(recipe).isNotNull();
        assertThat(recipe.getGenerator()).isNotNull();
        assertThat(recipe.getGenerator().getName()).isEqualTo("syft-generator");
        assertThat(recipe.getGenerator().getVersion()).isEqualTo("1.5.0");
        assertThat(recipe.getEnhancers()).isEmpty();
    }

    @Test
    void shouldBuildRecipeForRpm() {
        // When
        Recipe recipe = recipeBuilder.buildRecipeFor("RPM", "some-rpm-identifier");

        // Then
        assertThat(recipe).isNotNull();
        assertThat(recipe.getGenerator()).isNotNull();
        assertThat(recipe.getGenerator().getName()).isEqualTo("cyclonedx-maven-plugin");
        assertThat(recipe.getGenerator().getVersion()).isEqualTo("2.7.9");
        assertThat(recipe.getEnhancers()).hasSize(1);
        assertThat(recipe.getEnhancers().get(0).getName()).isEqualTo("rpm-enhancer");
        assertThat(recipe.getEnhancers().get(0).getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldHandleCaseInsensitiveTargetType() {
        // When
        Recipe recipe1 = recipeBuilder.buildRecipeFor("container_image", "image:tag");
        Recipe recipe2 = recipeBuilder.buildRecipeFor("CONTAINER_IMAGE", "image:tag");
        Recipe recipe3 = recipeBuilder.buildRecipeFor("rpm", "rpm-id");

        // Then
        assertThat(recipe1.getGenerator().getName()).isEqualTo("syft-generator");
        assertThat(recipe2.getGenerator().getName()).isEqualTo("syft-generator");
        assertThat(recipe3.getGenerator().getName()).isEqualTo("cyclonedx-maven-plugin");
    }

    @Test
    void shouldThrowExceptionForUnsupportedType() {
        // When/Then
        assertThatThrownBy(() -> recipeBuilder.buildRecipeFor("UNSUPPORTED_TYPE", "some-id"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported target type: UNSUPPORTED_TYPE");
    }

    @Test
    void shouldMatchMockRecipeBuilderBehaviorForRpm() {
        // This test ensures the ConfigurableRecipeBuilder produces the same result
        // as the MockRecipeBuilder for RPM type

        // When
        Recipe recipe = recipeBuilder.buildRecipeFor("RPM", "test-rpm");

        // Then - matches MockRecipeBuilder logic
        assertThat(recipe.getGenerator().getName()).isEqualTo("cyclonedx-maven-plugin");
        assertThat(recipe.getGenerator().getVersion()).isEqualTo("2.7.9");
        assertThat(recipe.getEnhancers()).hasSize(1);
        assertThat(recipe.getEnhancers().get(0).getName()).isEqualTo("rpm-enhancer");
        assertThat(recipe.getEnhancers().get(0).getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldMatchMockRecipeBuilderBehaviorForContainerImage() {
        // This test ensures the ConfigurableRecipeBuilder produces the same result
        // as the MockRecipeBuilder for CONTAINER_IMAGE type

        // When
        Recipe recipe = recipeBuilder.buildRecipeFor("CONTAINER_IMAGE", "test-image");

        // Then - matches MockRecipeBuilder logic
        assertThat(recipe.getGenerator().getName()).isEqualTo("syft-generator");
        assertThat(recipe.getGenerator().getVersion()).isEqualTo("1.5.0");
        assertThat(recipe.getEnhancers()).isEmpty();
    }

    @Test
    void shouldBuildRecipeWithDifferentIdentifiers() {
        // Verify that different identifiers don't affect the recipe selection
        // (only type matters for recipe selection)

        // When
        Recipe recipe1 = recipeBuilder.buildRecipeFor("CONTAINER_IMAGE", "image1:v1");
        Recipe recipe2 = recipeBuilder.buildRecipeFor("CONTAINER_IMAGE", "image2:v2");

        // Then
        assertThat(recipe1.getGenerator().getName()).isEqualTo(recipe2.getGenerator().getName());
        assertThat(recipe1.getGenerator().getVersion()).isEqualTo(recipe2.getGenerator().getVersion());
    }
}
