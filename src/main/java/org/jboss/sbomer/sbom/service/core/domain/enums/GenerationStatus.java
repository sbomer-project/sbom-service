package org.jboss.sbomer.sbom.service.core.domain.enums;

public enum GenerationStatus {
    /** Generation created but not yet scheduled */
    NEW,

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
}
