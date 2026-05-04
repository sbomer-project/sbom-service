package org.jboss.sbomer.sbom.service.core.domain.enums;

/**
 * Indicates who primarily owns or is responsible for resolving an error.
 *
 * This helps route issues to the appropriate system for resolution.
 */
public enum ErrorOwnership {
    /**
     * Error is caused by invalid client input or usage.
     * Client should correct their request.
     */
    CLIENT,

    /**
     * Error is caused by service-level issues (bugs, logic errors, config).
     * Service team should investigate and fix.
     */
    SERVICE,

    /**
     * Error is caused by external system failures (generators, enhancers).
     * External system owners should investigate.
     */
    EXTERNAL_SYSTEM,

    /**
     * Error is caused by platform infrastructure issues (database, messaging, etc).
     * Platform team should investigate.
     */
    PLATFORM
}
