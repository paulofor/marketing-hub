package com.marketinghub.experiment.monitoring.dto;

import java.time.Instant;
import java.util.Map;

/** Resume analytics do PDE usados para medir interação e intenção comercial. */
public record PostDeployPdeSummaryDto(
        boolean available,
        String status,
        String errorMessage,
        long totalEvents,
        long uniqueVisitors,
        long sessions,
        long pdeEntries,
        long pageViews,
        long presenceMapClicks,
        long diagnosticClicks,
        long fieldFilled,
        long loginStarted,
        long loginCompleted,
        long paywallViewed,
        long subscriptionClicked,
        long checkoutStarted,
        long subscriptionApproved,
        long totalVisibleMs,
        Instant lastEventAt,
        Map<String, Long> events
) {}
