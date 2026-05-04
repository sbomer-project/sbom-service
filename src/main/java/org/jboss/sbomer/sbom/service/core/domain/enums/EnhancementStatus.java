package org.jboss.sbomer.sbom.service.core.domain.enums;

public enum EnhancementStatus {
    /** Enhancement created but not yet scheduled */
    PENDING,

    /** Enhancement retry queued, waiting to be scheduled (internal only) */
    PENDING_RETRY,

    /** Enhancement is actively running */
    ENHANCING,

    /** Enhancement completed successfully */
    COMPLETED,

    /** Enhancement failed (after all retry attempts) */
    FAILED;

    public boolean isFinal() {
        return this == COMPLETED || this == FAILED;
    }

    /**
     * Check if this status represents a retry attempt
     */
    public boolean isRetry() {
        return this == PENDING_RETRY;
    }

    /**
     * Check if this status represents a pending state (initial or retry)
     */
    public boolean isPending() {
        return this == PENDING || this == PENDING_RETRY;
    }
}
