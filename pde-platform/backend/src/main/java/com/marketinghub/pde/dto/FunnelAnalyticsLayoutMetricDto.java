package com.marketinghub.pde.dto;

/** Resume o desempenho comercial de um layout público da experiência PDE. */
public record FunnelAnalyticsLayoutMetricDto(
        String layoutKey,
        long totalEvents,
        long sessions,
        long pdeEntries,
        long diagnosticClicks,
        long videoPartial,
        long videoComplete,
        long paywallViewed,
        long checkoutStarted,
        long subscriptionApproved
) {}
