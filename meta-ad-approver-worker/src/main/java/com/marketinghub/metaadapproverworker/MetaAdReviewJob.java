package com.marketinghub.metaadapproverworker;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Responsabilidade: representar uma revisão reservada no backend sem duplicar o modelo de dados.
 */
public record MetaAdReviewJob(Long creativeId, Long experimentId, Map<String, Object> context) {
  /** Constrói o job preservando integralmente o snapshot recebido. */
  public static MetaAdReviewJob from(Map<String, Object> value) {
    return new MetaAdReviewJob(
        number(value.get("creativeId")),
        number(value.get("experimentId")),
        Collections.unmodifiableMap(new LinkedHashMap<>(value)));
  }

  /** Converte identificadores numéricos do JSON em Long. */
  private static Long number(Object value) {
    if (!(value instanceof Number number))
      throw new IllegalArgumentException("Job sem identificador");
    return number.longValue();
  }
}
