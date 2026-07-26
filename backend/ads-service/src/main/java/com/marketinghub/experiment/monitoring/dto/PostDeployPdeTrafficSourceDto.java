package com.marketinghub.experiment.monitoring.dto;

/** Resume desempenho do PDE por UTM/criativo no painel pós-deploy. */
public record PostDeployPdeTrafficSourceDto(
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
