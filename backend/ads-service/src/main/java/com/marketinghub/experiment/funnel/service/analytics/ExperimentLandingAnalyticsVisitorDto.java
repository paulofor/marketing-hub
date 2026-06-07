package com.marketinghub.experiment.funnel.service.analytics;

import java.time.Instant;

/**
 * Resume a recorrência de um visitante provável identificado por navegador/dispositivo first-party.
 */
public record ExperimentLandingAnalyticsVisitorDto(
        String visitorId,
        long totalSessions,
        long validPageViews,
        Instant firstAccessAt,
        Instant lastAccessAt,
        long intervalSeconds,
        long distinctPages,
        String lastUserAgent,
        String deviceType,
        String deviceLabel,
        boolean recurrent) {
}
