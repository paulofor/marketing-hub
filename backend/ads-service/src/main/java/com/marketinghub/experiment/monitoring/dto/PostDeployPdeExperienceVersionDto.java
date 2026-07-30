package com.marketinghub.experiment.monitoring.dto;

/** Resume desempenho de uma versão comercial do PDE no painel pós-deploy. */
public record PostDeployPdeExperienceVersionDto(
    String experienceVersion,
    long totalEvents,
    long sessions,
    long pdeEntries,
    long firstInteractionClicks,
    long videoPartial,
    long videoComplete,
    long loginStarted,
    long paywallViewed,
    long checkoutIntent,
    long subscriptionApproved) {}
