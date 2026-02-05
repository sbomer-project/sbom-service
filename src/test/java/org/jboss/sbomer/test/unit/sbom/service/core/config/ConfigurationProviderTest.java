package org.jboss.sbomer.test.unit.sbom.service.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.jboss.sbomer.sbom.service.core.config.ConfigurationProvider;
import org.jboss.sbomer.sbom.service.core.config.recipe.RecipeConfig;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ConfigurationProviderTest {

    @Inject
    ConfigurationProvider configurationProvider;

    @Test
    void shouldLoadConfigurationSuccessfully() {
        // The configuration should be loaded during @PostConstruct
        assertThat(configurationProvider).isNotNull();
    }

    @Test
    void shouldReturnRecipeForContainerImage() {
        // When
        RecipeConfig recipe = configurationProvider.getRecipeForTargetType("CONTAINER_IMAGE");

        // Then
        assertThat(recipe).isNotNull();
        assertThat(recipe.getType()).isEqualTo("CONTAINER_IMAGE");
        assertThat(recipe.getGenerator()).isNotNull();
        assertThat(recipe.getGenerator().getName()).isEqualTo("syft-generator");
        assertThat(recipe.getGenerator().getVersion()).isEqualTo("1.5.0");
        assertThat(recipe.getEnhancers()).isEmpty();
    }

    @Test
    void shouldReturnRecipeForRpm() {
        // When
        RecipeConfig recipe = configurationProvider.getRecipeForTargetType("RPM");

        // Then
        assertThat(recipe).isNotNull();
        assertThat(recipe.getType()).isEqualTo("RPM");
        assertThat(recipe.getGenerator()).isNotNull();
        assertThat(recipe.getGenerator().getName()).isEqualTo("cyclonedx-maven-plugin");
        assertThat(recipe.getGenerator().getVersion()).isEqualTo("2.7.9");
        assertThat(recipe.getEnhancers()).hasSize(1);
        assertThat(recipe.getEnhancers().get(0).getName()).isEqualTo("rpm-enhancer");
        assertThat(recipe.getEnhancers().get(0).getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldBeCaseInsensitiveForTargetType() {
        // When
        RecipeConfig recipe1 = configurationProvider.getRecipeForTargetType("container_image");
        RecipeConfig recipe2 = configurationProvider.getRecipeForTargetType("CONTAINER_IMAGE");
        RecipeConfig recipe3 = configurationProvider.getRecipeForTargetType("rpm");

        // Then
        assertThat(recipe1.getType()).isEqualTo("CONTAINER_IMAGE");
        assertThat(recipe2.getType()).isEqualTo("CONTAINER_IMAGE");
        assertThat(recipe3.getType()).isEqualTo("RPM");
    }

    @Test
    void shouldThrowExceptionForUnsupportedType() {
        // When/Then
        assertThatThrownBy(() -> configurationProvider.getRecipeForTargetType("UNKNOWN_TYPE"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported target type: UNKNOWN_TYPE");
    }

    @Test
    void shouldThrowExceptionForNullType() {
        // When/Then
        assertThatThrownBy(() -> configurationProvider.getRecipeForTargetType(null))
            .isInstanceOf(NullPointerException.class);
    }
}
