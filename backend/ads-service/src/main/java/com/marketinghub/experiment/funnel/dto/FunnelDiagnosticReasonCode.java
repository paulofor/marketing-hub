package com.marketinghub.experiment.funnel.dto;

public enum FunnelDiagnosticReasonCode {
    NO_ATTEMPTS,
    LOW_SAMPLE_SIZE,
    SEQUENTIAL_INCONSISTENCY,
    RULE_OF_THREE_FAILED,
    RULE_OF_THREE_STILL_INCONCLUSIVE,
    BELOW_MIN_RATE,
    HEALTHY_OR_INCONCLUSIVE
}
