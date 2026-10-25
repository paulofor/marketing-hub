package com.marketinghub.experimentstrategistworker;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.slf4j.LoggerFactory;

/** Valida a ligação entre referência, decisão estratégica e entrega verificável da modelagem. */
final class ReferenceModelingContract {
  /** Impede aprovar modelagem sem rastreabilidade; tarefas históricas preservam seu contrato. */
  static void validate(JsonNode result, JsonNode task) {
    if (task == null) return;
    JsonNode context;
    try {
      JsonNode raw = task.path("processContextJson");
      context = raw.isObject() ? raw : new ObjectMapper().readTree(raw.asText("{}"));
    } catch (Exception ex) {
      LoggerFactory.getLogger(ReferenceModelingContract.class)
          .error("Contexto de modelagem inválido. taskId={}", task.path("id"), ex);
      throw new IllegalArgumentException("Contexto de modelagem inválido.", ex);
    }
    JsonNode modeling = context.path("referenceModeling");
    if (modeling.isNull() || modeling.isMissingNode()) return;
    if (!"MARKET_REFERENCE_MODELING_V1".equals(modeling.path("contractVersion").asText()))
      throw new IllegalArgumentException("Versão da modelagem inválida.");
    if (!"APPROVE".equals(result.path("decision").asText())) return;
    JsonNode trace = result.path("referenceModelingTrace");
    if (!trace.isArray() || trace.isEmpty() || result.path("alternatives").size() != 3)
      throw new IllegalArgumentException(
          "Modelagem exige três alternativas e rastreabilidade da decisão.");
    Set<String> cited = new HashSet<>();
    modeling.path("factualSources").forEach(s -> cited.add(s.path("url").asText()));
    for (JsonNode item : trace) {
      for (String field :
          List.of(
              "sourceUrl",
              "observedFact",
              "limitation",
              "learning",
              "decision",
              "deliverable",
              "acceptanceMetric"))
        if (!item.path(field).isTextual() || item.path(field).asText().isBlank())
          throw new IllegalArgumentException("Rastreabilidade incompleta: " + field);
      if (!cited.contains(item.path("sourceUrl").asText()))
        throw new IllegalArgumentException("A decisão cita fonte ausente do resultado factual.");
    }
    if ("INTERNATIONAL_TO_BR_V1".equals(modeling.path("researchScope").asText()))
      validateInternational(result.path("internationalAssessment"), modeling);
  }

  /**
   * Exige sinais atribuídos e adaptação brasileira sem transformar concorrência em prova própria.
   */
  private static void validateInternational(JsonNode assessment, JsonNode modeling) {
    JsonNode references = assessment.path("references");
    if (!assessment.isObject() || !references.isArray() || references.isEmpty())
      throw new IllegalArgumentException(
          "A modelagem internacional exige evidências e adaptação ao Brasil.");
    Map<String, List<String>> observations = new HashMap<>();
    modeling
        .path("factualSources")
        .forEach(
            source ->
                observations
                    .computeIfAbsent(source.path("url").asText(), key -> new ArrayList<>())
                    .add(source.path("observation").asText()));
    Set<String> expected = new HashSet<>();
    modeling.path("referenceUrls").forEach(url -> expected.add(url.asText()));
    Set<String> assessed = new HashSet<>();
    for (JsonNode reference : references) {
      for (String field :
          List.of("referenceUrl", "sourceUrl", "observedEvidence", "originMarket", "limitation"))
        requireText(reference, field);
      String identity = reference.path("referenceUrl").asText();
      if (!expected.contains(identity) || !assessed.add(identity))
        throw new IllegalArgumentException(
            "Referência internacional ausente da preparação ou duplicada.");
      String quote = reference.path("observedEvidence").asText();
      if (quote.length() < 15
          || observations.getOrDefault(reference.path("sourceUrl").asText(), List.of()).stream()
              .noneMatch(observation -> observation.contains(quote)))
        throw new IllegalArgumentException(
            "O sinal internacional precisa de trecho literal da evidência preservada.");
      if (!Set.of("ADOPTION", "PAID_OFFER", "REPORTED_REVENUE", "RETENTION", "UNKNOWN")
              .contains(reference.path("signalType").asText())
          || !Set.of("SELLER_CLAIM", "PUBLIC_CUSTOMER_REPORT", "INDEPENDENT_REPORT", "UNKNOWN")
              .contains(reference.path("evidenceLevel").asText()))
        throw new IllegalArgumentException(
            "Classifique o sinal e a autoria sem presumir sucesso comercial.");
    }
    if (!assessed.equals(expected))
      throw new IllegalArgumentException(
          "Avalie cada referência; informação ausente deve permanecer desconhecida.");
    for (String field :
        List.of(
            "recognizedDesire",
            "firstValue",
            "localization",
            "localAlternatives",
            "paidValue",
            "economicsHypotheses",
            "mainRisk",
            "validationPlan")) requireText(assessment.path("brazilAdaptation"), field);
  }

  /** Recusa omissões em decisões que precisam orientar a entrega e as métricas seguintes. */
  private static void requireText(JsonNode value, String field) {
    if (!value.path(field).isTextual() || value.path(field).asText().isBlank())
      throw new IllegalArgumentException("Adaptação internacional incompleta: " + field);
  }
}
