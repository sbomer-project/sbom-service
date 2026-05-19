package org.jboss.sbomer.sbom.service.core.port.spi;

import java.util.Set;

import org.jboss.sbomer.events.orchestration.Recipe;

/**
 * <p>
 * To create a recipe for an SBOM generation
 * </p>
 */
public interface RecipeBuilder {

    /**
     * Specify an available generator + enhancers for a given type and identifier
     * 
     * @param type The target type (e.g., "CONTAINER_IMAGE", "RPM")
     * @param identifier The target identifier
     * @return The built recipe
     * @throws IllegalArgumentException if no recipe exists for the type
     */
    Recipe buildRecipeFor(String type, String identifier);

    /**
     * Check if a recipe exists for the given target type.
     * 
     * @param type The target type to check
     * @return true if a recipe is configured for this type
     */
    boolean hasRecipeFor(String type);

    /**
     * Get all supported target types.
     * 
     * @return Set of supported target type names
     */
    Set<String> getSupportedTypes();

}
