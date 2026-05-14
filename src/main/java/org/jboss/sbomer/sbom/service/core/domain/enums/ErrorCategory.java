package org.jboss.sbomer.sbom.service.core.domain.enums;

/**
 * High-level categorization of errors for grouping and operational classification.
 * 
 * Categories help organize error codes into logical domains and support
 * consistent handling, monitoring, and troubleshooting across the service.
 */
public enum ErrorCategory {
    /**
     * Errors related to invalid client input or request validation.
     * Examples: invalid target format, missing required fields, invalid state transitions.
     */
    VALIDATION,
    
    /**
     * Errors related to service configuration or platform dependencies.
     * Examples: missing config files, invalid configuration, unavailable dependencies.
     */
    CONFIGURATION,
    
    /**
     * Errors in orchestration and workflow coordination logic.
     * Examples: scheduling failures, state rollup errors, retry policy issues.
     */
    ORCHESTRATION,
    
    /**
     * Errors from external worker execution (generators, enhancers).
     * Examples: generator failures, enhancer timeouts, resource exhaustion.
     */
    EXTERNAL_EXECUTION,
    
    /**
     * Errors in persistence layer or internal infrastructure.
     * Examples: database errors, transaction failures, unexpected internal errors.
     */
    INTERNAL
}
