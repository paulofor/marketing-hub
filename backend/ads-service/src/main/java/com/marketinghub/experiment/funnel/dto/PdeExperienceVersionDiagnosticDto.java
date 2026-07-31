package com.marketinghub.experiment.funnel.dto;

/** Contrato de uma versão PDE disponível no summary usado pelo diagnóstico do cockpit. */
public record PdeExperienceVersionDiagnosticDto(
    String experienceVersion,
    long totalEvents,
    long sessions,
    long pdeEntries,
    long videoPartial,
    long videoComplete,
    long loginStarted,
    long paywallViewed,
    long checkoutIntent,
    long subscriptionApproved) {}
