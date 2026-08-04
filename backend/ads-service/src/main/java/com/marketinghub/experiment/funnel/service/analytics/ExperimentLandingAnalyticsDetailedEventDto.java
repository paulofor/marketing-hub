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
    String trafficQuality,
    String trafficQualityReason,
    Map<String, String> attributes) {

  /** Mantém compatibilidade com produtores anteriores que ainda não classificavam o tráfego. */
  public ExperimentLandingAnalyticsDetailedEventDto(
      Long eventId,
      String anonymousVisitorId,
      String anonymousSessionId,
      String eventType,
      String sectionId,
      Instant occurredAt,
      Map<String, String> attributes) {
    this(
        eventId,
        anonymousVisitorId,
        anonymousSessionId,
        eventType,
        sectionId,
        occurredAt,
        "UNKNOWN",
        "LEGACY_UNCLASSIFIED",
        attributes);
  }
}
