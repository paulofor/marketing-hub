package com.marketinghub.experiment;

/**
 * Estados operacionais de um experimento comercial.
 */
public enum ExperimentStatus {
    PLANNED,
    RUNNING,
    PAUSED,
    STANDBY,
    USER_STOPPED,
    VALIDATED,
    INVALIDATED,
    INCONCLUSIVE,
    FINISHED,
    FAILED
}
