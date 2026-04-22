package com.marketinghub.experiment;

/**
 * Current status of an experiment.
 */
public enum ExperimentStatus {
    PLANNED,
    RUNNING,
    PAUSED,
    USER_STOPPED,
    VALIDATED,
    INVALIDATED,
    INCONCLUSIVE,
    FINISHED,
    FAILED
}
