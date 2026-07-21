package com.marketinghub.experiment.monitoring.pde;

import java.util.List;

/** Contrato mínimo do resumo de analytics retornado pelo backend PDE. */
public record PdeAnalyticsSummary(
        String productSlug,
        String currentExperienceVersion,
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
        List<PdeEventMetric> events,
        List<PdeExperienceVersionMetric> experienceVersions
) {
    /** Representa a contagem agregada por tipo de evento do PDE. */
    public record PdeEventMetric(String eventType, long total) {}

    /** Representa a contagem comercial agregada por versão da experiência PDE. */
    public record PdeExperienceVersionMetric(
            String experienceVersion,
            long totalEvents,
            long sessions,
            long pdeEntries,
            long presenceMapClicks,
            long diagnosticClicks,
            long loginStarted,
            long paywallViewed,
            long subscriptionClicked,
            long checkoutStarted,
            long subscriptionApproved
    ) {}
}
