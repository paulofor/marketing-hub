package com.marketinghub.experiment.funnel.dto;

import java.util.List;

/** Contrato de diagnóstico operacional da integração PDE usada pelo cockpit do experimento. */
public record ExperimentPdeCockpitDiagnosticsDto(
    Long experimentId,
    boolean pdeMembershipFunnel,
    String followUpActionUrl,
    String normalizedDomain,
    String requestedPdeBaseUrl,
    String productSlug,
    boolean pdeSummaryLoaded,
    String currentExperienceVersion,
    String expectedExperienceVersion,
    String expectedExperienceVersionSource,
    String versionTokenFallback,
    String matchedExperienceVersion,
    boolean attributionFilterApplied,
    List<String> attributionCodes,
    int matchedTrafficSources,
    long matchedTrafficSessions,
    boolean fallbackUsed,
    String fallbackReason,
    List<PdeExperienceVersionDiagnosticDto> availableExperienceVersions,
    String errorType,
    String errorMessage) {}
