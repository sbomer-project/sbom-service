package org.jboss.sbomer.test.unit.sbom.service.core.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.jboss.sbomer.events.common.GenerationRequestSpec;
import org.jboss.sbomer.events.common.Target;
import org.jboss.sbomer.sbom.service.core.port.spi.RecipeBuilder;
import org.jboss.sbomer.sbom.service.core.utility.RequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequestValidatorTest {

    private RecipeBuilder recipeBuilder;
    private RequestValidator validator;

    @BeforeEach
    void setUp() {
        recipeBuilder = mock(RecipeBuilder.class);
        validator = new RequestValidator(recipeBuilder);
    }

    @Test
    void testValidate_ValidRequests_ReturnsValid() {
        // Given
        List<GenerationRequestSpec> specs = List.of(
            createValidSpec("CONTAINER_IMAGE", "quay.io/test:v1")
        );
        when(recipeBuilder.hasRecipeFor("CONTAINER_IMAGE")).thenReturn(true);

        // When
        RequestValidator.ValidationResult result = validator.validate(specs);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void testValidate_InvalidType_ReturnsInvalid() {
        // Given
        List<GenerationRequestSpec> specs = List.of(
            createValidSpec("NONEXISTENT", "test")
        );
        when(recipeBuilder.hasRecipeFor("NONEXISTENT")).thenReturn(false);

        // When
        RequestValidator.ValidationResult result = validator.validate(specs);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getTargetType()).isEqualTo("NONEXISTENT");
        assertThat(result.getErrors().get(0).getReason()).contains("Unsupported target type");
    }



    @Test
    void testValidate_EmptyTargetType_ReturnsInvalid() {
        // Given
        List<GenerationRequestSpec> specs = List.of(
            createValidSpec("", "test")
        );

        // When
        RequestValidator.ValidationResult result = validator.validate(specs);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getReason()).contains("Target type cannot be null or empty");
    }



    @Test
    void testValidate_EmptyTargetIdentifier_ReturnsInvalid() {
        // Given
        List<GenerationRequestSpec> specs = List.of(
            createValidSpec("CONTAINER_IMAGE", "")
        );

        // When
        RequestValidator.ValidationResult result = validator.validate(specs);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getReason()).contains("Target identifier cannot be null or empty");
    }

    @Test
    void testValidate_EmptyList_ReturnsInvalid() {
        // Given
        List<GenerationRequestSpec> specs = List.of();

        // When
        RequestValidator.ValidationResult result = validator.validate(specs);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getReason()).contains("At least one generation request is required");
    }

    @Test
    void testValidate_NullList_ReturnsInvalid() {
        // Given
        List<GenerationRequestSpec> specs = null;

        // When
        RequestValidator.ValidationResult result = validator.validate(specs);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getReason()).contains("At least one generation request is required");
    }

    @Test
    void testValidate_MixedValidInvalid_ReturnsInvalid() {
        // Given
        List<GenerationRequestSpec> specs = List.of(
            createValidSpec("CONTAINER_IMAGE", "quay.io/test:v1"),
            createValidSpec("NONEXISTENT", "test"),
            createValidSpec("RPM", "test-rpm")
        );
        when(recipeBuilder.hasRecipeFor("CONTAINER_IMAGE")).thenReturn(true);
        when(recipeBuilder.hasRecipeFor("NONEXISTENT")).thenReturn(false);
        when(recipeBuilder.hasRecipeFor("RPM")).thenReturn(true);

        // When
        RequestValidator.ValidationResult result = validator.validate(specs);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getIndex()).isEqualTo(1);
        assertThat(result.getErrors().get(0).getTargetType()).isEqualTo("NONEXISTENT");
    }

    @Test
    void testValidate_MultipleInvalid_ReturnsAllErrors() {
        // Given
        List<GenerationRequestSpec> specs = List.of(
            createValidSpec("INVALID1", "test1"),
            createValidSpec("INVALID2", "test2")
        );
        when(recipeBuilder.hasRecipeFor("INVALID1")).thenReturn(false);
        when(recipeBuilder.hasRecipeFor("INVALID2")).thenReturn(false);

        // When
        RequestValidator.ValidationResult result = validator.validate(specs);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors().get(0).getIndex()).isEqualTo(0);
        assertThat(result.getErrors().get(1).getIndex()).isEqualTo(1);
    }

    @Test
    void testGetSupportedTypes_ReturnsSupportedTypes() {
        // Given
        Set<String> supportedTypes = Set.of("CONTAINER_IMAGE", "RPM", "MAVEN");
        when(recipeBuilder.getSupportedTypes()).thenReturn(supportedTypes);

        // When
        Set<String> result = validator.getSupportedTypes();

        // Then
        assertThat(result).containsExactlyInAnyOrder("CONTAINER_IMAGE", "RPM", "MAVEN");
    }

    @Test
    void testFormatValidationErrors_SingleError() {
        // Given
        List<GenerationRequestSpec> specs = List.of(
            createValidSpec("INVALID_TYPE", "test")
        );
        when(recipeBuilder.hasRecipeFor("INVALID_TYPE")).thenReturn(false);
        when(recipeBuilder.getSupportedTypes()).thenReturn(Set.of("CONTAINER_IMAGE", "RPM"));

        // When
        RequestValidator.ValidationResult result = validator.validate(specs);
        String formatted = validator.formatValidationErrors(result);

        // Then
        assertThat(formatted).contains("Request validation failed with 1 error(s)");
        assertThat(formatted).contains("Request[0]: Unsupported target type: INVALID_TYPE");
        assertThat(formatted).contains("type=INVALID_TYPE");
        assertThat(formatted).contains("Supported types: [");
        assertThat(formatted).contains("CONTAINER_IMAGE");
        assertThat(formatted).contains("RPM");
    }

    @Test
    void testFormatValidationErrors_MultipleErrors() {
        // Given
        List<GenerationRequestSpec> specs = List.of(
            createValidSpec("INVALID1", "test1"),
            createValidSpec("INVALID2", "test2")
        );
        when(recipeBuilder.hasRecipeFor("INVALID1")).thenReturn(false);
        when(recipeBuilder.hasRecipeFor("INVALID2")).thenReturn(false);
        when(recipeBuilder.getSupportedTypes()).thenReturn(Set.of("CONTAINER_IMAGE"));

        // When
        RequestValidator.ValidationResult result = validator.validate(specs);
        String formatted = validator.formatValidationErrors(result);

        // Then
        assertThat(formatted).contains("Request validation failed with 2 error(s)");
        assertThat(formatted).contains("Request[0]");
        assertThat(formatted).contains("Request[1]");
        assertThat(formatted).contains("; "); // Separator between errors
    }

    private GenerationRequestSpec createValidSpec(String type, String identifier) {
        Target target = Target.newBuilder()
            .setType(type)
            .setIdentifier(identifier)
            .build();
        
        return GenerationRequestSpec.newBuilder()
            .setGenerationId("test-id")
            .setTarget(target)
            .build();
    }
}
