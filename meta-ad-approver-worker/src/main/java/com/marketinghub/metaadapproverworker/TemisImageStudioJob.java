package com.marketinghub.metaadapproverworker;

import java.util.List;
import java.util.Map;

/** Responsabilidade: representar uma criação ou edição visual reservada para Têmis. */
public record TemisImageStudioJob(
    Long jobId,
    Long commercialPlanId,
    String operation,
    String prompt,
    String label,
    List<String> purposes,
    String size,
    String quality,
    List<String> referenceImageUrls,
    String producerExecutionId) {
  /** Converte o contrato genérico do backend em job validado do executor. */
  public static TemisImageStudioJob from(Map<String, Object> value) {
    return new TemisImageStudioJob(
        number(value.get("jobId")),
        number(value.get("commercialPlanId")),
        text(value.get("operation")),
        text(value.get("prompt")),
        text(value.get("label")),
        strings(value.get("purposes")),
        text(value.get("size")),
        text(value.get("quality")),
        strings(value.get("referenceImageUrls")),
        text(value.get("producerExecutionId")));
  }

  /** Converte um identificador numérico obrigatório. */
  private static Long number(Object value) {
    if (!(value instanceof Number number)) {
      throw new IllegalArgumentException("Job visual sem identificador");
    }
    return number.longValue();
  }

  /** Normaliza um campo textual obrigatório. */
  private static String text(Object value) {
    return value == null ? "" : value.toString().trim();
  }

  /** Normaliza uma lista textual recebida do backend. */
  private static List<String> strings(Object value) {
    if (!(value instanceof java.util.Collection<?> collection)) {
      return List.of();
    }
    return collection.stream()
        .map(TemisImageStudioJob::text)
        .filter(item -> !item.isBlank())
        .toList();
  }
}
