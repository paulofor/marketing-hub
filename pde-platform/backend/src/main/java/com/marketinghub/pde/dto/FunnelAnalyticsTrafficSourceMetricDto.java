package com.marketinghub.pde.dto;

/** Resume desempenho do PDE por origem, campanha e criativo identificados por UTM. */
public record FunnelAnalyticsTrafficSourceMetricDto(
        String trafficChannel,
        String utmSource,
        String utmMedium,
        String utmCampaign,
        String utmContent,
        long sessions,
        long pdeEntries,
        long firstInteractionClicks,
        long loginStarted,
        long paywallViewed,
        long checkoutStarted,
        long subscriptionApproved,
        double firstInteractionRate,
        double paywallRate,
        double checkoutRate,
        double purchaseRate,
        long totalVisibleMs,
        String lastEventAt
) {}
