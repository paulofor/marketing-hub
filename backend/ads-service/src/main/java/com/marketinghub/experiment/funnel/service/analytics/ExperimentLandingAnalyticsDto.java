package com.marketinghub.experiment.funnel.service.analytics;

import java.time.Instant;
import java.util.List;

/**
 * Consolida as métricas de analytics da landing vinculada ao experimento.
 */
public record ExperimentLandingAnalyticsDto(
        long totalEvents,
        long totalSessions,
        long pageViews,
        long sectionViewEvents,
        long totalVisibleMs,
        long averageVisibleMsPerSession,
        Instant lastEventAt,
        List<ExperimentLandingAnalyticsDeviceDto> deviceBreakdown,
        List<ExperimentLandingAnalyticsOperatingSystemDto> mobileOperatingSystemBreakdown,
        List<ExperimentLandingAnalyticsScreenSizeDto> screenSizeBreakdown,
        ExperimentLandingAnalyticsVisitorsDto visitors,
        List<ExperimentLandingAnalyticsSessionDto> sessions) {
}
