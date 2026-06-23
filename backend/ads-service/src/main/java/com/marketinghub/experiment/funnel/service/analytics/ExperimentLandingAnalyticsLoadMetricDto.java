package com.marketinghub.experiment.funnel.service.analytics;

/**
 * Consolida indicadores técnicos de carregamento capturados pela landing publicada.
 */
public record ExperimentLandingAnalyticsLoadMetricDto(
        long events,
        long averageLoadDurationMs,
        long p95LoadDurationMs,
        long averageDomContentLoadedMs,
        long averageFirstContentfulPaintMs,
        long totalResourceErrors,
        long sessionsWithoutSectionEvents,
        double initialEngagementRate,
        long inAppBrowserSessions,
        double inAppBrowserPercentage,
        String diagnosisCode,
        String diagnosisLabel,
        String diagnosisSeverity,
        String diagnosisSummary,
        String recommendation) {
}
