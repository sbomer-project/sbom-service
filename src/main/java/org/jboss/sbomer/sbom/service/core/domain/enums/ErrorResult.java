package org.jboss.sbomer.sbom.service.core.domain.enums;

import lombok.Getter;

/**
 * Canonical service-owned error result codes.
 *
 * These codes represent stable, high-level error classifications that are independent
 * of external tool implementations (for example generator and enhancer workers). Each
 * code includes metadata about category, retryability, ownership, and severity so the
 * service can make consistent decisions for retries, persistence, REST responses, and
 * Kafka notifications.
 *
 * Legacy worker result codes such as {@code ERR_OOM}, {@code ERR_SYSTEM},
 * {@code ERR_GENERATION}, and {@code ERR_ENHANCEMENT} are translated into this enum by
 * {@code ErrorMapper}. External reasons such as {@code TaskRunFailed} should be stored
 * as diagnostic details, not used as primary error identities.
 *
 * Example mappings:
 * <ul>
 *   <li>{@code ERR_OOM -> EXTERNAL_RESOURCE_EXHAUSTED}</li>
 *   <li>{@code ERR_SYSTEM -> EXTERNAL_SYSTEM_ERROR}</li>
 *   <li>{@code ERR_INDEX_INVALID -> INVALID_TARGET}</li>
 *   <li>{@code ERR_GENERATION -> GENERATOR_EXECUTION_FAILED}</li>
 *   <li>{@code ERR_ENHANCEMENT -> ENHANCER_EXECUTION_FAILED}</li>
 * </ul>
 */
@Getter
public enum ErrorResult {
    
    // ==================== VALIDATION AND CLIENT INPUT ====================
    
    /**
     * Generic invalid request error.
     *
     * Example: REST payload validation fails before workflow orchestration starts.
     */
    INVALID_REQUEST(
        ErrorCategory.VALIDATION,
        false,
        ErrorOwnership.CLIENT,
        ErrorSeverity.WARN
    ),
    
    /**
     * Invalid target specification (e.g., malformed image reference, invalid build ID).
     *
     * Example: mapped from {@code ERR_INDEX_INVALID} when an external worker reports an invalid target.
     */
    INVALID_TARGET(
        ErrorCategory.VALIDATION,
        false,
        ErrorOwnership.CLIENT,
        ErrorSeverity.WARN
    ),
    
    /**
     * Invalid recipe configuration or selection.
     */
    INVALID_RECIPE(
        ErrorCategory.VALIDATION,
        false,
        ErrorOwnership.CLIENT,
        ErrorSeverity.WARN
    ),
    
    /**
     * Attempted state transition is not allowed (e.g., retrying a completed enhancement).
     */
    INVALID_STATE_TRANSITION(
        ErrorCategory.VALIDATION,
        false,
        ErrorOwnership.CLIENT,
        ErrorSeverity.WARN
    ),
    
    /**
     * Requested entity does not exist.
     */
    ENTITY_NOT_FOUND(
        ErrorCategory.VALIDATION,
        false,
        ErrorOwnership.CLIENT,
        ErrorSeverity.WARN
    ),
    
    // ==================== CONFIGURATION AND PLATFORM SETUP ====================
    
    /**
     * Required configuration is missing.
     */
    CONFIG_MISSING(
        ErrorCategory.CONFIGURATION,
        false,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Configuration is present but invalid.
     */
    CONFIG_INVALID(
        ErrorCategory.CONFIGURATION,
        false,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Required dependency or service is unavailable.
     */
    DEPENDENCY_UNAVAILABLE(
        ErrorCategory.CONFIGURATION,
        true,
        ErrorOwnership.PLATFORM,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Schema registry error (connection, schema not found, etc).
     */
    SCHEMA_REGISTRY_ERROR(
        ErrorCategory.CONFIGURATION,
        true,
        ErrorOwnership.PLATFORM,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Failed to deserialize incoming message.
     */
    MESSAGE_DESERIALIZATION_ERROR(
        ErrorCategory.CONFIGURATION,
        false,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Failed to serialize outgoing message.
     */
    MESSAGE_SERIALIZATION_ERROR(
        ErrorCategory.CONFIGURATION,
        false,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    ),
    
    // ==================== ORCHESTRATION AND WORKFLOW LOGIC ====================
    
    /**
     * Error processing a request at the orchestration level.
     */
    REQUEST_PROCESSING_ERROR(
        ErrorCategory.ORCHESTRATION,
        false,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Failed to schedule a generation.
     */
    GENERATION_SCHEDULING_ERROR(
        ErrorCategory.ORCHESTRATION,
        true,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Failed to schedule an enhancement.
     */
    ENHANCEMENT_SCHEDULING_ERROR(
        ErrorCategory.ORCHESTRATION,
        true,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Error during state rollup calculation.
     */
    ROLLUP_STATE_ERROR(
        ErrorCategory.ORCHESTRATION,
        false,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Error evaluating retry policy.
     */
    RETRY_POLICY_ERROR(
        ErrorCategory.ORCHESTRATION,
        false,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Error executing a retry attempt.
     */
    RETRY_EXECUTION_ERROR(
        ErrorCategory.ORCHESTRATION,
        true,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    ),
    
    // ==================== EXTERNAL WORKER AND TOOL EXECUTION ====================
    
    /**
     * Generator execution failed.
     *
     * Example: mapped from {@code ERR_GENERATION}, {@code ERR_POST}, {@code ERR_MULTI}, or generic worker failures.
     */
    GENERATOR_EXECUTION_FAILED(
        ErrorCategory.EXTERNAL_EXECUTION,
        true,
        ErrorOwnership.EXTERNAL_SYSTEM,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Enhancer execution failed.
     *
     * Example: mapped from {@code ERR_ENHANCEMENT} or a generic enhancement worker failure.
     */
    ENHANCER_EXECUTION_FAILED(
        ErrorCategory.EXTERNAL_EXECUTION,
        true,
        ErrorOwnership.EXTERNAL_SYSTEM,
        ErrorSeverity.ERROR
    ),
    
    /**
     * External worker timed out.
     *
     * Example: use when a generator or enhancer exceeds its execution window or callback deadline.
     */
    EXTERNAL_TIMEOUT(
        ErrorCategory.EXTERNAL_EXECUTION,
        true,
        ErrorOwnership.EXTERNAL_SYSTEM,
        ErrorSeverity.ERROR
    ),
    
    /**
     * External worker ran out of resources (OOM, disk space, etc).
     *
     * Example: mapped from {@code ERR_OOM} when a worker exhausts memory or ephemeral storage.
     */
    EXTERNAL_RESOURCE_EXHAUSTED(
        ErrorCategory.EXTERNAL_EXECUTION,
        true,
        ErrorOwnership.EXTERNAL_SYSTEM,
        ErrorSeverity.ERROR
    ),
    
    /**
     * External system error (infrastructure, platform issues).
     *
     * Example: mapped from {@code ERR_SYSTEM} when the worker platform or underlying runtime fails.
     */
    EXTERNAL_SYSTEM_ERROR(
        ErrorCategory.EXTERNAL_EXECUTION,
        true,
        ErrorOwnership.EXTERNAL_SYSTEM,
        ErrorSeverity.ERROR
    ),
    
    /**
     * External worker received bad configuration.
     *
     * Example: mapped from {@code ERR_CONFIG_INVALID} or {@code ERR_CONFIG_MISSING}.
     */
    EXTERNAL_BAD_CONFIGURATION(
        ErrorCategory.EXTERNAL_EXECUTION,
        false,
        ErrorOwnership.EXTERNAL_SYSTEM,
        ErrorSeverity.ERROR
    ),
    
    // ==================== PERSISTENCE AND INTERNAL INFRASTRUCTURE ====================
    
    /**
     * Database operation failed.
     *
     * Example: persistence of request, generation, enhancement, or run state fails due to a transient database issue.
     */
    DATABASE_ERROR(
        ErrorCategory.INTERNAL,
        true,
        ErrorOwnership.PLATFORM,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Transaction failed or rolled back.
     */
    TRANSACTION_ERROR(
        ErrorCategory.INTERNAL,
        true,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Internal processing error (service logic bug).
     */
    INTERNAL_PROCESSING_ERROR(
        ErrorCategory.INTERNAL,
        false,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    ),
    
    /**
     * Unexpected error (catch-all for unknown failures).
     *
     * Example: an exception escapes normal classification and is mapped as a defensive fallback.
     */
    UNEXPECTED_ERROR(
        ErrorCategory.INTERNAL,
        false,
        ErrorOwnership.SERVICE,
        ErrorSeverity.ERROR
    );
    
    private final ErrorCategory category;
    private final boolean retryable;
    private final ErrorOwnership ownership;
    private final ErrorSeverity severity;
    
    ErrorResult(ErrorCategory category, boolean retryable, ErrorOwnership ownership, ErrorSeverity severity) {
        this.category = category;
        this.retryable = retryable;
        this.ownership = ownership;
        this.severity = severity;
    }
}
