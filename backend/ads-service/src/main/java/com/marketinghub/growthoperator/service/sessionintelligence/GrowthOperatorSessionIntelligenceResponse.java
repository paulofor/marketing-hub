package com.marketinghub.growthoperator.service.sessionintelligence;

import java.time.Instant;
import java.util.Map;

/** Expõe a leitura canônica do experimento com disponibilidade, origem e evidências separadas. */
public record GrowthOperatorSessionIntelligenceResponse(
    String contractVersion,
    boolean available,
    String primarySource,
    Long experimentId,
    Instant consultedAt,
    int requestedEventLimit,
    int appliedEventLimit,
    Object landingAnalytics,
    Object personalizedSampleDelivery,
    Object pdeAnalytics) {
  /** Preserva o contrato anterior por plano na nova rota tipada usada pelo BPM. */
  public static GrowthOperatorSessionIntelligenceResponse from(Map<String, Object> value) {
    return new GrowthOperatorSessionIntelligenceResponse(
        (String) value.get("contractVersion"),
        Boolean.TRUE.equals(value.get("available")),
        (String) value.get("primarySource"),
        (Long) value.get("experimentId"),
        (Instant) value.get("consultedAt"),
        (Integer) value.get("requestedEventLimit"),
        (Integer) value.get("appliedEventLimit"),
        value.get("landingAnalytics"),
        value.get("personalizedSampleDelivery"),
        value.get("pdeAnalytics"));
  }
}
