package com.marketinghub.pde.dto;

/** Resume desempenho do PDE por origem, campanha e criativo identificados por UTM. */
public record FunnelAnalyticsTrafficSourceMetricDto(
        String utmSource,
        String utmCampaign,
        String utmContent,
        long sessions,
        long pdeEntries,
        long firstInteractionClicks,
        long loginStarted,
        long paywallViewed,
        long checkoutStarted,
        long subscriptionApproved,
        long totalVisibleMs,
        String lastEventAt
) {}
