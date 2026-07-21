package com.marketinghub.experiment.monitoring.pde;

import java.util.List;

/** Contrato mínimo do resumo de analytics retornado pelo backend PDE. */
public record PdeAnalyticsSummary(
        String productSlug,
        long totalEvents,
        long uniqueVisitors,
        long sessions,
        long pedEntries,
        long pageViews,
        long loginStarted,
        long loginCompleted,
        long paywallViewed,
        long subscriptionClicked,
        long subscriptionApproved,
        long accessReleased,
        long firstUse,
        long checkoutStarted,
        long totalVisibleMs,
        List<PdeEventMetric> events
) {
    /** Representa a contagem agregada por tipo de evento do PDE. */
    public record PdeEventMetric(String eventType, long total) {}
}
