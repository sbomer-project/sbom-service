package org.jboss.sbomer.sbom.service.core.domain.enums;

public enum GenerationStatus {
    /** Generation created but not yet scheduled */
    PENDING,

    /** Generation retry queued, waiting to be scheduled (internal only) */
    PENDING_RETRY,

    /** Generation is actively running */
    GENERATING,

    /** Generation completed successfully */
    COMPLETED,

    /** Generation failed (after all retry attempts) */
    FAILED;

    public static GenerationStatus fromName(String phase) {
        return GenerationStatus.valueOf(phase.toUpperCase());
    }

    public String toName() {
        return this.name().toUpperCase();
    }

    public boolean isOlderThan(GenerationStatus desiredStatus) {
        if (desiredStatus == null) {
            return false;
        }

        return desiredStatus.ordinal() > this.ordinal();
    }

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
