package com.marketinghub.experiment.funnel.service.analytics;

import java.util.List;

/**
 * Consolida visitantes prováveis e recorrentes capturados pela landing do experimento.
 */
public record ExperimentLandingAnalyticsVisitorsDto(
        long probableVisitors,
        long recurrentVisitors,
        long singleVisitVisitors,
        List<ExperimentLandingAnalyticsVisitorDto> visitors) {
}
