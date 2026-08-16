package com.marketinghub.metaadapproverworker;

import java.util.List;
import java.util.Map;

/** Responsabilidade: representar um entregável reservado para revisão independente de Têmis. */
public record TemisLibraryReviewJob(
    Long assetId,
    Long jobId,
    Long commercialPlanId,
    String planName,
    String offer,
    String targetAudience,
    String assetUrl,
    String label,
    List<String> purposes,
    String producerExecutionId,
    Map<String, Object> context) {
  /** Constrói o job preservando o snapshot completo para auditoria. */
  public static TemisLibraryReviewJob from(Map<String, Object> value) {
    return new TemisLibraryReviewJob(
        number(value.get("assetId")),
        number(value.get("jobId")),
        number(value.get("commercialPlanId")),
        text(value.get("planName")),
        text(value.get("offer")),
        text(value.get("targetAudience")),
        text(value.get("assetUrl")),
        text(value.get("label")),
        strings(value.get("purposes")),
        text(value.get("producerExecutionId")),
        java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(value)));
  }

  /** Converte identificador numérico obrigatório. */
  private static Long number(Object value) {
    if (!(value instanceof Number number)) throw new IllegalArgumentException("Revisão sem ID");
    return number.longValue();
  }

  /** Normaliza texto opcional. */
  private static String text(Object value) {
    return value == null ? "" : value.toString().trim();
  }

  /** Normaliza finalidades do entregável. */
  private static List<String> strings(Object value) {
    if (!(value instanceof java.util.Collection<?> collection)) return List.of();
    return collection.stream()
        .map(TemisLibraryReviewJob::text)
        .filter(item -> !item.isBlank())
        .toList();
  }
}
