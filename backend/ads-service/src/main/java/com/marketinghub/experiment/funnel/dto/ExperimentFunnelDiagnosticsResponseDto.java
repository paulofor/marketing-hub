package com.marketinghub.experiment.funnel.dto;

import java.util.List;

public record ExperimentFunnelDiagnosticsResponseDto(
        List<ExperimentFunnelStageDiagnosticDto> diagnostics,
        String contextualAlert
) {
}
