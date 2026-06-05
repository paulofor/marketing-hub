package com.marketinghub.experiment.funnel.service.analytics;

/**
 * Resume o tempo visível acumulado em uma seção da landing durante uma sessão.
 */
public record ExperimentLandingAnalyticsSectionDto(
        String sectionId,
        long visibleMs,
        long events) {
}
