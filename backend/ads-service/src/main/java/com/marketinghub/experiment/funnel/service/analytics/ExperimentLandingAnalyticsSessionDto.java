package com.marketinghub.experiment.funnel.service.analytics;

import java.time.Instant;
import java.util.List;

/**
 * Representa uma sessão identificada pelo script público de analytics da landing.
 */
public record ExperimentLandingAnalyticsSessionDto(
        String sessionId,
        long eventCount,
        long pageViews,
        long sectionViewEvents,
        long totalVisibleMs,
        Instant firstEventAt,
        Instant lastEventAt,
        String lastPageUrl,
        String lastUserAgent,
        List<ExperimentLandingAnalyticsSectionDto> topSections) {
}
