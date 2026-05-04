package org.jboss.sbomer.sbom.service.core.domain.enums;

/**
 * Severity level for errors, used for logging and operational alerting.
 * 
 * This helps determine the appropriate logging level and urgency of response.
 */
public enum ErrorSeverity {
    /**
     * Informational - no action required, normal operation.
     */
    INFO,
    
    /**
     * Warning - potential issue but operation can continue.
     * Typically used for client errors or recoverable issues.
     */
    WARN,
    
    /**
     * Error - operation failed, requires attention.
     * Used for service errors, external failures, and infrastructure issues.
     */
    ERROR
}
