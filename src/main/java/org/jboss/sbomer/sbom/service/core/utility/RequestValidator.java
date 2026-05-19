package org.jboss.sbomer.sbom.service.core.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.sbomer.events.common.GenerationRequestSpec;
import org.jboss.sbomer.sbom.service.core.port.spi.RecipeBuilder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Validates generation requests before processing.
 * Used by both REST and Kafka adapters to ensure only valid requests
 * are passed to the core service layer.
 */
@ApplicationScoped
public class RequestValidator {

    private final RecipeBuilder recipeBuilder;

    @Inject
    public RequestValidator(RecipeBuilder recipeBuilder) {
        this.recipeBuilder = recipeBuilder;
    }

    /**
     * Validates a list of generation request specifications.
     */
    public ValidationResult validate(List<GenerationRequestSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return new ValidationResult(false, List.of(
                new ValidationError(-1, null, null, "At least one generation request is required")
            ));
        }

        List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < specs.size(); i++) {
            validateSpec(specs.get(i), i).ifPresent(errors::add);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private java.util.Optional<ValidationError> validateSpec(GenerationRequestSpec spec, int index) {
        if (spec.getTarget() == null) {
            return java.util.Optional.of(new ValidationError(index, null, null, "Target cannot be null"));
        }

        String type = spec.getTarget().getType();
        String id = spec.getTarget().getIdentifier();

        if (isBlank(type)) {
            return java.util.Optional.of(new ValidationError(index, type, id, "Target type cannot be null or empty"));
        }
        if (isBlank(id)) {
            return java.util.Optional.of(new ValidationError(index, type, id, "Target identifier cannot be null or empty"));
        }
        if (!recipeBuilder.hasRecipeFor(type)) {
            return java.util.Optional.of(new ValidationError(index, type, id, "Unsupported target type: " + type));
        }

        return java.util.Optional.empty();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Get all supported target types for error messages.
     */
    public Set<String> getSupportedTypes() {
        return recipeBuilder.getSupportedTypes();
    }

    /**
     * Format validation errors into a human-readable message.
     * 
     * @param validationResult The validation result containing errors
     * @return Formatted error message with all validation failures
     */
    public String formatValidationErrors(ValidationResult validationResult) {
        String errorDetails = validationResult.getErrors().stream()
            .map(error -> String.format("Request[%d]: %s%s",
                error.getIndex(),
                error.getReason(),
                error.getTargetType() != null ? " (type=" + error.getTargetType() + ")" : ""))
            .collect(java.util.stream.Collectors.joining("; "));
        
        return String.format("Request validation failed with %d error(s): %s. Supported types: %s",
            validationResult.getErrors().size(),
            errorDetails,
            getSupportedTypes());
    }

    @Data
    @AllArgsConstructor
    public static class ValidationResult {
        private final boolean valid;
        private final List<ValidationError> errors;
    }

    @Data
    @AllArgsConstructor
    public static class ValidationError {
        private final int index;
        private final String targetType;
        private final String targetIdentifier;
        private final String reason;
    }
}
