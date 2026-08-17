package com.marketinghub.metaadapproverworker;

import java.util.Map;

/** Responsabilidade: representar uma amostra congelada de replay visual reservada pelo backend. */
public record TemisVisualLearningJob(
    Long runId,
    String contextKey,
    String baselineVersion,
    String candidateVersion,
    Map<String, Object> input,
    String producerExecutionId) {

  /** Converte o contrato genérico do backend em execução correlacionada. */
  @SuppressWarnings("unchecked")
  public static TemisVisualLearningJob from(Map<String, Object> value) {
    Object input = value.get("input");
    return new TemisVisualLearningJob(
        number(value.get("runId")),
        text(value.get("contextKey")),
        text(value.get("baselineVersion")),
        text(value.get("candidateVersion")),
        input instanceof Map<?, ?> ? (Map<String, Object>) input : Map.of(),
        text(value.get("producerExecutionId")));
  }

  /** Converte identificador obrigatório. */
  private static Long number(Object value) {
    if (!(value instanceof Number number)) throw new IllegalArgumentException("Replay sem runId");
    return number.longValue();
  }

  /** Normaliza um texto do contrato. */
  private static String text(Object value) {
    return value == null ? "" : value.toString().trim();
  }
}
