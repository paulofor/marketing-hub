package com.marketinghub.experiment.dto;

import java.util.List;

/**
 * Resume o tempo medio de sessao da landing do experimento para exibicao na lista administrativa.
 */
public record ExperimentSessionDurationSummaryDto(
        long totalSessions,
        long averageVisibleMsPerSession,
        List<ExperimentSessionDurationVariantDto> variants) {
}
