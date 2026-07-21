package com.marketinghub.experiment.monitoring.dto;

/** Resume desempenho do PDE por UTM/criativo no painel pós-deploy. */
public record PostDeployPdeTrafficSourceDto(
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
