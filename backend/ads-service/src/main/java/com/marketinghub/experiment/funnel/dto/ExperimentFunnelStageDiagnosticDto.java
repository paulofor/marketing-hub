package com.marketinghub.experiment.funnel.dto;

import com.marketinghub.experiment.funnel.ExperimentFunnelStage;

public record ExperimentFunnelStageDiagnosticDto(
        ExperimentFunnelStage stageKey,
        String stageLabel,
        long attempts,
        long successes,
        Double observedRate,
        Double minAcceptableRate,
        Double upper95RateIfZero,
        java.util.List<FunnelThresholdCheckDto> thresholdChecks,
        FunnelDiagnosticStatus status,
        FunnelDiagnosticReasonCode reasonCode,
        String message,
        boolean technicalIssueSuspected
) {
}
