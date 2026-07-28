package com.marketinghub.pde.dto;

/** Resume o desempenho de uma versão comercial da experiência PDE. */
public record FunnelAnalyticsExperienceVersionMetricDto(
        String experienceVersion,
        long totalEvents,
        long sessions,
        long pdeEntries,
        long presenceMapClicks,
        long diagnosticClicks,
        long videoPartial,
        long videoComplete,
        long loginStarted,
        long paywallViewed,
        long subscriptionClicked,
        long checkoutStarted,
        long subscriptionApproved
) {}
