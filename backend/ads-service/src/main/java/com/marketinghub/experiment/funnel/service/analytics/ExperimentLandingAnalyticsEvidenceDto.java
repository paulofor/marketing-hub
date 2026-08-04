package com.marketinghub.experiment.funnel.service.analytics;

import java.util.List;

/** Reúne resumo, jornadas e eventos detalhados congelados para diagnóstico auditável. */
public record ExperimentLandingAnalyticsEvidenceDto(
    Long experimentId,
    long totalEventsAvailable,
    int includedEvents,
    boolean truncated,
    ExperimentLandingAnalyticsDto summary,
    List<ExperimentLandingAnalyticsDetailedEventDto> detailedEvents) {}
