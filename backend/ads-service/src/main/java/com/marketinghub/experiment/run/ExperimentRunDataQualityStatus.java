package com.marketinghub.experiment.run;

/**
 * Resume a qualidade dos dados disponíveis para uma execução de experimento.
 */
public enum ExperimentRunDataQualityStatus {
    UNKNOWN,
    VALID,
    WARNING,
    BLOCKED,
    STALE
}
