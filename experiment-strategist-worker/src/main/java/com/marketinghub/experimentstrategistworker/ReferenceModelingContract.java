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
  }
}
