package com.marketinghub.experiment.funnel.service.analytics;

/**
 * Resume a participação de um tipo de dispositivo nos acessos da landing.
 */
public record ExperimentLandingAnalyticsDeviceDto(
        String deviceType,
        String label,
        long sessions,
        double percentage) {
}
