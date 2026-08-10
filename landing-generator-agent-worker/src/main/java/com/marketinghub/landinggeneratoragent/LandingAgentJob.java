package com.marketinghub.landinggeneratoragent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Responsabilidade: representar um snapshot segregado reservado no backend. */
public record LandingAgentJob(String executionId, Long experimentId, Map<String, Object> context) {
  /** Constrói o job preservando o contexto congelado. */
  public static LandingAgentJob from(Map<String, Object> value) {
    Object id = value.get("executionId");
    Object experiment = value.get("experimentId");
    if (!(id instanceof String text) || !(experiment instanceof Number number)) {
      throw new IllegalArgumentException("Job do Agente de Landing sem identificadores");
    }
    Object rawContext = value.get("context");
    Map<String, Object> context =
        rawContext instanceof Map<?, ?> map
            ? new LinkedHashMap<>((Map<String, Object>) map)
            : new LinkedHashMap<>();
    return new LandingAgentJob(text, number.longValue(), Map.copyOf(context));
  }
}
