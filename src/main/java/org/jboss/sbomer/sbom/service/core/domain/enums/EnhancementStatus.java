package org.jboss.sbomer.sbom.service.core.domain.enums;

public enum EnhancementStatus {
    /** Enhancement created but not yet scheduled */
    PENDING,

    /** Enhancement is actively running */
    ENHANCING,

    /** Enhancement completed successfully */
    COMPLETED,

    /** Enhancement failed (after all retry attempts) */
    FAILED;

    public boolean isFinal() {
        return this == COMPLETED || this == FAILED;
    }
}
