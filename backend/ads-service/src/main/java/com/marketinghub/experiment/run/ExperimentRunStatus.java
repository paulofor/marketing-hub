package com.marketinghub.experiment.run;

/**
 * Representa o estado operacional de uma tentativa de execução do experimento.
 */
public enum ExperimentRunStatus {
    DRAFT,
    PREFLIGHT_PENDING,
    PREFLIGHT_RUNNING,
    PREFLIGHT_FAILED,
    READY_TO_PUBLISH,
    PUBLICATION_PENDING,
    PUBLISHING,
    PUBLISHED_AWAITING_EXPOSURE,
    RUNNING,
    PAUSE_REQUESTED,
    PAUSED,
    STOP_REQUESTED,
    COMPLETED,
    FAILED,
    CANCELLED
}
