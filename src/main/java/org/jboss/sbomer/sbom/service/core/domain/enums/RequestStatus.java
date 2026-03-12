package org.jboss.sbomer.sbom.service.core.domain.enums;

public enum RequestStatus {
    /** Request received, generations not yet started */
    PENDING,

    /** At least one generation is actively processing */
    PROCESSING,

    /** All generations completed successfully */
    COMPLETED,

    /** All generations failed */
    FAILED
}
