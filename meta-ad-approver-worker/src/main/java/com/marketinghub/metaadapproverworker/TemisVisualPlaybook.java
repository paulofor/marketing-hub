package com.marketinghub.metaadapproverworker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsabilidade: representar o aprendizado promovido e exemplos congelados para uma produção.
 */
public record TemisVisualPlaybook(
    String version,
    String contextKey,
    String status,
    List<String> promotedRules,
    List<String> avoid,
    List<Example> approvedExamples) {

  /** Converte o contrato do backend sem aceitar campos de outro contexto por inferência. */
  public static TemisVisualPlaybook from(Object value) {
    if (!(value instanceof Map<?, ?> map)) {
      return new TemisVisualPlaybook("", "", "", List.of(), List.of(), List.of());
    }
    return new TemisVisualPlaybook(
        text(map.get("version")),
        text(map.get("contextKey")),
        text(map.get("status")),
        strings(map.get("promotedRules")),
        strings(map.get("avoid")),
        examples(map.get("approvedExamples")));
  }

  /** Entrega as URLs positivas sem duplicação para composição multimodal. */
  public List<String> exampleUrls() {
    return approvedExamples.stream()
        .map(Example::assetUrl)
        .filter(value -> !value.isBlank())
        .distinct()
        .toList();
  }

  /** Converte o playbook em mapa estruturado para auditoria e prompt. */
  public Map<String, Object> audit() {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("version", version);
    value.put("contextKey", contextKey);
    value.put("status", status);
    value.put("promotedRules", promotedRules);
    value.put("avoid", avoid);
    value.put("approvedExamples", approvedExamples);
    return value;
  }

  /** Normaliza os exemplos estruturados recebidos do backend. */
  private static List<Example> examples(Object value) {
    if (!(value instanceof java.util.Collection<?> collection)) return List.of();
    List<Example> result = new ArrayList<>();
    for (Object item : collection) {
      if (item instanceof Map<?, ?> map) {
        result.add(
            new Example(
                number(map.get("assetId")),
                text(map.get("label")),
                text(map.get("assetUrl")),
                text(map.get("format")),
                strings(map.get("purposes"))));
      }
    }
    return List.copyOf(result);
  }

  /** Normaliza uma lista textual. */
  private static List<String> strings(Object value) {
    if (!(value instanceof java.util.Collection<?> collection)) return List.of();
    return collection.stream()
        .map(TemisVisualPlaybook::text)
        .filter(item -> !item.isBlank())
        .distinct()
        .toList();
  }

  /** Normaliza um texto opcional. */
  private static String text(Object value) {
    return value == null ? "" : value.toString().trim();
  }

  /** Normaliza um identificador opcional de exemplo. */
  private static Long number(Object value) {
    return value instanceof Number number ? number.longValue() : null;
  }

  /** Responsabilidade: identificar um exemplo aprovado sem duplicar seu binário. */
  public record Example(
      Long assetId, String label, String assetUrl, String format, List<String> purposes) {}
}
