package com.marketinghub.experiment.funnel.service.analytics;

/**
 * Resume a participação de um sistema operacional mobile nas sessões da landing.
 */
public record ExperimentLandingAnalyticsOperatingSystemDto(
        String operatingSystem,
        String label,
        long sessions,
        double percentage) {
}
