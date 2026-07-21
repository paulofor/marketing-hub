package com.marketinghub.experiment.monitoring.dto;

/** Classifica a decisão operacional sugerida pelo painel pós-deploy. */
public enum PostDeployMonitorDecision {
    WAITING_DATA,
    KEEP_MONITORING,
    PAUSE_AND_FIX,
    SCALE_GRADUALLY,
    TECHNICAL_ATTENTION
}
