package org.jboss.sbomer.sbom.service.core.domain.enums;

// we may not use all these in the end, it might depend on the generators
public enum GenerationStatus {
    NEW, SCHEDULED, GENERATING, FINISHED, FAILED;

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
        return this.equals(FAILED) || this.equals(FINISHED);
    }
}
