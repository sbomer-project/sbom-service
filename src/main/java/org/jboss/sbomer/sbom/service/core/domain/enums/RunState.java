package org.jboss.sbomer.sbom.service.core.domain.enums;

/**
 * Represents the execution state of a background worker run.
 * This is separate from domain lifecycle states.
 */
public enum RunState {
    /** Run is queued but not yet started */
    PENDING,
    
    /** Run is currently executing */
    RUNNING,
    
    /** Run completed successfully */
    SUCCEEDED,
    
    /** Run failed with an error */
    FAILED;
    
    public boolean isFinal() {
        return this == SUCCEEDED || this == FAILED;
    }
}

