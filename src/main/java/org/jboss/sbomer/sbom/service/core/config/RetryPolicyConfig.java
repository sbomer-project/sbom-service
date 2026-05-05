package org.jboss.sbomer.sbom.service.core.config;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.sbomer.sbom.service.core.domain.enums.ErrorResult;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for automatic retry policies using canonical error codes.
 * 
 * This class manages retry behavior for failed operations based on canonical ErrorResult codes.
 * Configuration is loaded from application.properties using the following format:
 * 
 * <pre>
 * # Global retry toggle
 * sbomer.retry.enabled=true
 * 
 * # Canonical error code retry configuration
 * sbomer.retry.error.external-resource-exhausted.max-attempts=3
 * sbomer.retry.error.external-system-error.max-attempts=5
 * sbomer.retry.error.generator-execution-failed.max-attempts=3
 * </pre>
 * 
 * Error codes with max-attempts=0 or not configured will not be retried.
 */
@ApplicationScoped
@Slf4j
public class RetryPolicyConfig {

    @ConfigProperty(name = "sbomer.retry.enabled", defaultValue = "false")
    boolean retryEnabled;

    private final Map<ErrorResult, Integer> errorMaxAttempts = new HashMap<>();

    /**
     * Loads retry configuration from application.properties on startup.
     * 
     * Default configuration for retryable errors (infrastructure and transient failures):
     * - EXTERNAL_RESOURCE_EXHAUSTED: 3 attempts
     * - EXTERNAL_SYSTEM_ERROR: 5 attempts
     * - EXTERNAL_TIMEOUT: 3 attempts
     * - GENERATOR_EXECUTION_FAILED: 3 attempts
     * - ENHANCER_EXECUTION_FAILED: 3 attempts
     * - DATABASE_ERROR: 3 attempts
     * - DEPENDENCY_UNAVAILABLE: 5 attempts
     * - SCHEMA_REGISTRY_ERROR: 3 attempts
     * - GENERATION_SCHEDULING_ERROR: 3 attempts
     * - ENHANCEMENT_SCHEDULING_ERROR: 3 attempts
     * - RETRY_EXECUTION_ERROR: 2 attempts
     * - TRANSACTION_ERROR: 3 attempts
     * 
     * Non-retryable errors (configuration, validation, and permanent failures) default to 0 attempts.
     */
    @PostConstruct
    void loadConfiguration() {
        log.info("Loading retry policy configuration...");

        // Retryable errors - infrastructure and transient failures
        errorMaxAttempts.put(
                ErrorResult.EXTERNAL_RESOURCE_EXHAUSTED,
                getConfigValue("sbomer.retry.error.external-resource-exhausted.max-attempts", 3));
        errorMaxAttempts.put(
                ErrorResult.EXTERNAL_SYSTEM_ERROR,
                getConfigValue("sbomer.retry.error.external-system-error.max-attempts", 5));
        errorMaxAttempts.put(
                ErrorResult.EXTERNAL_TIMEOUT,
                getConfigValue("sbomer.retry.error.external-timeout.max-attempts", 3));
        errorMaxAttempts.put(
                ErrorResult.GENERATOR_EXECUTION_FAILED,
                getConfigValue("sbomer.retry.error.generator-execution-failed.max-attempts", 3));
        errorMaxAttempts.put(
                ErrorResult.ENHANCER_EXECUTION_FAILED,
                getConfigValue("sbomer.retry.error.enhancer-execution-failed.max-attempts", 3));
        errorMaxAttempts.put(
                ErrorResult.DATABASE_ERROR,
                getConfigValue("sbomer.retry.error.database-error.max-attempts", 3));
        errorMaxAttempts.put(
                ErrorResult.DEPENDENCY_UNAVAILABLE,
                getConfigValue("sbomer.retry.error.dependency-unavailable.max-attempts", 5));
        errorMaxAttempts.put(
                ErrorResult.SCHEMA_REGISTRY_ERROR,
                getConfigValue("sbomer.retry.error.schema-registry-error.max-attempts", 3));
        errorMaxAttempts.put(
                ErrorResult.GENERATION_SCHEDULING_ERROR,
                getConfigValue("sbomer.retry.error.generation-scheduling-error.max-attempts", 3));
        errorMaxAttempts.put(
                ErrorResult.ENHANCEMENT_SCHEDULING_ERROR,
                getConfigValue("sbomer.retry.error.enhancement-scheduling-error.max-attempts", 3));
        errorMaxAttempts.put(
                ErrorResult.RETRY_EXECUTION_ERROR,
                getConfigValue("sbomer.retry.error.retry-execution-error.max-attempts", 2));
        errorMaxAttempts.put(
                ErrorResult.TRANSACTION_ERROR,
                getConfigValue("sbomer.retry.error.transaction-error.max-attempts", 3));

        // Non-retryable errors (explicitly set to 0)
        errorMaxAttempts.put(
                ErrorResult.INVALID_REQUEST,
                getConfigValue("sbomer.retry.error.invalid-request.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.INVALID_TARGET,
                getConfigValue("sbomer.retry.error.invalid-target.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.INVALID_RECIPE,
                getConfigValue("sbomer.retry.error.invalid-recipe.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.INVALID_STATE_TRANSITION,
                getConfigValue("sbomer.retry.error.invalid-state-transition.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.ENTITY_NOT_FOUND,
                getConfigValue("sbomer.retry.error.entity-not-found.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.CONFIG_MISSING,
                getConfigValue("sbomer.retry.error.config-missing.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.CONFIG_INVALID,
                getConfigValue("sbomer.retry.error.config-invalid.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.EXTERNAL_BAD_CONFIGURATION,
                getConfigValue("sbomer.retry.error.external-bad-configuration.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.MESSAGE_DESERIALIZATION_ERROR,
                getConfigValue("sbomer.retry.error.message-deserialization-error.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.MESSAGE_SERIALIZATION_ERROR,
                getConfigValue("sbomer.retry.error.message-serialization-error.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.REQUEST_PROCESSING_ERROR,
                getConfigValue("sbomer.retry.error.request-processing-error.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.ROLLUP_STATE_ERROR,
                getConfigValue("sbomer.retry.error.rollup-state-error.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.RETRY_POLICY_ERROR,
                getConfigValue("sbomer.retry.error.retry-policy-error.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.INTERNAL_PROCESSING_ERROR,
                getConfigValue("sbomer.retry.error.internal-processing-error.max-attempts", 0));
        errorMaxAttempts.put(
                ErrorResult.UNEXPECTED_ERROR,
                getConfigValue("sbomer.retry.error.unexpected-error.max-attempts", 0));

        log.info(
                "Retry configuration loaded: enabled={}, canonical error policies={}",
                retryEnabled,
                errorMaxAttempts.size());

        if (log.isDebugEnabled()) {
            log.debug("Canonical error retry policies: {}", errorMaxAttempts);
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
     * Gets the maximum number of retry attempts configured for a canonical error code.
     * 
     * @param error the canonical error result
     * @return maximum number of attempts (0 means no retry)
     */
    public int getMaxAttemptsForError(ErrorResult error) {
        return errorMaxAttempts.getOrDefault(error, 0);
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