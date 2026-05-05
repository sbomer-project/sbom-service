package org.jboss.sbomer.sbom.service.core.config;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.sbomer.sbom.service.core.domain.enums.EnhancementResult;
import org.jboss.sbomer.sbom.service.core.domain.enums.GenerationResult;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for automatic retry policies.
 * 
 * This class manages retry behavior for failed generations and enhancements based on error types.
 * Configuration is loaded from application.properties using the following format:
 * 
 * <pre>
 * # Global retry toggle
 * sbomer.retry.enabled=true
 * 
 * # Generation error retry configuration
 * sbomer.retry.generation.err-oom.max-attempts=3
 * sbomer.retry.generation.err-system.max-attempts=5
 * 
 * # Enhancement error retry configuration
 * sbomer.retry.enhancement.err-general.max-attempts=2
 * </pre>
 * 
 * Error codes with max-attempts=0 or not configured will not be retried.
 */
@ApplicationScoped
@Slf4j
public class RetryPolicyConfig {

    @ConfigProperty(name = "sbomer.retry.enabled", defaultValue = "false")
    boolean retryEnabled;

    private final Map<GenerationResult, Integer> generationMaxAttempts = new HashMap<>();
    private final Map<EnhancementResult, Integer> enhancementMaxAttempts = new HashMap<>();

    /**
     * Loads retry configuration from application.properties on startup.
     * 
     * Default configuration:
     * - Retryable generation errors: ERR_OOM (3), ERR_SYSTEM (5), ERR_POST (3), ERR_GENERAL (2)
     * - Non-retryable generation errors: ERR_CONFIG_INVALID, ERR_CONFIG_MISSING, ERR_INDEX_INVALID,
     * ERR_GENERATION
     * - Retryable enhancement errors: ERR_GENERAL (2), ERR_ENHANCEMENT (3)
     * - Non-retryable enhancement errors: ERR_CONFIG_INVALID
     */
    @PostConstruct
    void loadConfiguration() {
        log.info("Loading retry policy configuration...");

        // Load generation retry configuration with defaults
        generationMaxAttempts.put(
                GenerationResult.ERR_OOM,
                getConfigValue("sbomer.retry.generation.err-oom.max-attempts", 3));
        generationMaxAttempts.put(
                GenerationResult.ERR_SYSTEM,
                getConfigValue("sbomer.retry.generation.err-system.max-attempts", 5));
        generationMaxAttempts.put(
                GenerationResult.ERR_POST,
                getConfigValue("sbomer.retry.generation.err-post.max-attempts", 3));
        generationMaxAttempts.put(
                GenerationResult.ERR_GENERAL,
                getConfigValue("sbomer.retry.generation.err-general.max-attempts", 2));

        // Non-retryable generation errors (default to 0)
        generationMaxAttempts.put(
                GenerationResult.ERR_CONFIG_INVALID,
                getConfigValue("sbomer.retry.generation.err-config-invalid.max-attempts", 0));
        generationMaxAttempts.put(
                GenerationResult.ERR_CONFIG_MISSING,
                getConfigValue("sbomer.retry.generation.err-config-missing.max-attempts", 0));
        generationMaxAttempts.put(
                GenerationResult.ERR_INDEX_INVALID,
                getConfigValue("sbomer.retry.generation.err-index-invalid.max-attempts", 0));
        generationMaxAttempts.put(
                GenerationResult.ERR_GENERATION,
                getConfigValue("sbomer.retry.generation.err-generation.max-attempts", 0));
        generationMaxAttempts.put(
                GenerationResult.ERR_MULTI,
                getConfigValue("sbomer.retry.generation.err-multi.max-attempts", 0));

        // Load enhancement retry configuration with defaults
        enhancementMaxAttempts.put(
                EnhancementResult.ERR_GENERAL,
                getConfigValue("sbomer.retry.enhancement.err-general.max-attempts", 2));
        enhancementMaxAttempts.put(
                EnhancementResult.ERR_ENHANCEMENT,
                getConfigValue("sbomer.retry.enhancement.err-enhancement.max-attempts", 3));

        // Non-retryable enhancement errors (default to 0)
        enhancementMaxAttempts.put(
                EnhancementResult.ERR_CONFIG_INVALID,
                getConfigValue("sbomer.retry.enhancement.err-config-invalid.max-attempts", 0));

        log.info(
                "Retry configuration loaded: enabled={}, generation policies={}, enhancement policies={}",
                retryEnabled,
                generationMaxAttempts.size(),
                enhancementMaxAttempts.size());

        if (log.isDebugEnabled()) {
            log.debug("Generation retry policies: {}", generationMaxAttempts);
            log.debug("Enhancement retry policies: {}", enhancementMaxAttempts);
        }
    }

    /**
     * Checks if automatic retry is globally enabled.
     * 
     * @return true if retry is enabled, false otherwise
     */
    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    /**
     * Gets the maximum number of retry attempts configured for a specific generation error.
     * 
     * @param result the generation error result
     * @return maximum number of attempts (0 means no retry)
     */
    public int getMaxAttemptsForGeneration(GenerationResult result) {
        return generationMaxAttempts.getOrDefault(result, 0);
    }

    /**
     * Gets the maximum number of retry attempts configured for a specific enhancement error.
     * 
     * @param result the enhancement error result
     * @return maximum number of attempts (0 means no retry)
     */
    public int getMaxAttemptsForEnhancement(EnhancementResult result) {
        return enhancementMaxAttempts.getOrDefault(result, 0);
    }



    /**
     * Reads an optional configuration value with a default fallback.
     * 
     * @param key the configuration key
     * @param defaultValue the default value if key is not present
     * @return the configured value or default
     */
    private int getConfigValue(String key, int defaultValue) {
        return ConfigProvider.getConfig().getOptionalValue(key, Integer.class).orElse(defaultValue);
    }
}
