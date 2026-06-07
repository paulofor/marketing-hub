package com.marketinghub.experiment.funnel.service.analytics;

/**
 * Resume a participação de uma resolução de tela nas sessões da landing.
 */
public record ExperimentLandingAnalyticsScreenSizeDto(
        String screenSize,
        String label,
        Integer width,
        Integer height,
        long sessions,
        double percentage) {
}
