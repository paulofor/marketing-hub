package com.marketinghub.experiment.funnel.service.analytics;

import java.time.Instant;
import java.util.Map;

/** Representa um evento detalhado e anonimizado entregue ao Operador de Crescimento. */
public record ExperimentLandingAnalyticsDetailedEventDto(
    Long eventId,
    String anonymousVisitorId,
    String anonymousSessionId,
    String eventType,
    String sectionId,
    Instant occurredAt,
    Map<String, String> attributes) {}
