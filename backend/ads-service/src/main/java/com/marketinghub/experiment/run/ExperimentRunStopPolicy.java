package com.marketinghub.experiment.run;

/**
 * Define a política operacional usada para encerrar ou pausar uma execução.
 */
public enum ExperimentRunStopPolicy {
    FIRST_VALID_LEAD_STANDBY,
    FIXED_WINDOW,
    MIN_SAMPLE_AND_WINDOW,
    STOP_LOSS,
    MANUAL_ONLY
}
