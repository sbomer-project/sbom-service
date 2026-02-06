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
        assertThat(recipe.getGenerator().getOptions()).isNotEmpty();
        assertThat(recipe.getGenerator().getOptions()).hasSize(2);
        assertThat(recipe.getGenerator().getOptions()).containsEntry("format", "cyclonedx-json");
        assertThat(recipe.getGenerator().getOptions()).containsEntry("scope", "all-layers");
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
        assertThat(recipe.getGenerator().getOptions()).isNotEmpty();
        assertThat(recipe.getGenerator().getOptions()).hasSize(2);
        assertThat(recipe.getGenerator().getOptions()).containsEntry("includeSystemScope", "true");
        assertThat(recipe.getGenerator().getOptions()).containsEntry("outputFormat", "json");
        assertThat(recipe.getEnhancers()).hasSize(1);
        assertThat(recipe.getEnhancers().get(0).getName()).isEqualTo("rpm-enhancer");
        assertThat(recipe.getEnhancers().get(0).getVersion()).isEqualTo("1.0.0");
        assertThat(recipe.getEnhancers().get(0).getOptions()).isNotEmpty();
        assertThat(recipe.getEnhancers().get(0).getOptions()).hasSize(2);
        assertThat(recipe.getEnhancers().get(0).getOptions()).containsEntry("enrichMetadata", "true");
        assertThat(recipe.getEnhancers().get(0).getOptions()).containsEntry("validateLicenses", "false");
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

    @Test
    void shouldHandleGeneratorOptionsCorrectly() {
        // When
        RecipeConfig recipe = configurationProvider.getRecipeForTargetType("CONTAINER_IMAGE");

        // Then
        assertThat(recipe.getGenerator().getOptions()).isNotEmpty();
        assertThat(recipe.getGenerator().getOptions()).hasSize(2);
        assertThat(recipe.getGenerator().getOptions().get("format")).isEqualTo("cyclonedx-json");
        assertThat(recipe.getGenerator().getOptions().get("scope")).isEqualTo("all-layers");
    }

    @Test
    void shouldHandleEnhancerOptionsCorrectly() {
        // When
        RecipeConfig recipe = configurationProvider.getRecipeForTargetType("RPM");

        // Then
        assertThat(recipe.getEnhancers()).isNotEmpty();
        assertThat(recipe.getEnhancers().get(0).getOptions()).isNotEmpty();
        assertThat(recipe.getEnhancers().get(0).getOptions()).hasSize(2);
        assertThat(recipe.getEnhancers().get(0).getOptions().get("enrichMetadata")).isEqualTo("true");
        assertThat(recipe.getEnhancers().get(0).getOptions().get("validateLicenses")).isEqualTo("false");
    }

    @Test
    void shouldHandleBothGeneratorAndEnhancerOptions() {
        // When
        RecipeConfig recipe = configurationProvider.getRecipeForTargetType("RPM");

        // Then - Generator options
        assertThat(recipe.getGenerator().getOptions()).isNotEmpty();
        assertThat(recipe.getGenerator().getOptions()).containsKeys("includeSystemScope", "outputFormat");

        // Then - Enhancer options
        assertThat(recipe.getEnhancers().get(0).getOptions()).isNotEmpty();
        assertThat(recipe.getEnhancers().get(0).getOptions()).containsKeys("enrichMetadata", "validateLicenses");
    }
}
