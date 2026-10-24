package com.marketinghub.communicationagentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Confere a proveniência das cinco decisões comerciais sem substituir o julgamento de Psique. */
final class PersuasionEvidenceContract {
  private static final Logger log = LoggerFactory.getLogger(PersuasionEvidenceContract.class);
  private static final Map<String, List<String>> METRICS =
      Map.of(
          "RECOGNIZED_DESIRE", List.of("AD_CLICK_THROUGH_RATE", "QUALIFIED_VISIT_RATE"),
          "EASY_FIRST_STEP", List.of("SAMPLE_VIEW_RATE", "FIRST_VALUE_COMPLETION_RATE"),
          "VALUE_BEFORE_COMMITMENT",
              List.of("SAMPLE_TO_CHECKOUT_RATE", "FIRST_VALUE_COMPLETION_RATE"),
          "PAID_INCREMENT", List.of("NET_PURCHASE_CONVERSION_RATE"),
          "REPEAT_WITH_MARGIN", List.of("NET_CONTRIBUTION_AFTER_ACQUISITION"));
  private static final List<String> SOURCES =
      List.of(
          "/marketStrategicContract/", "/communicationMaterializationContext/",
          "/learningSalesCycle/", "/productBigIdea/");

  /** Impede criar instâncias de um contrato sem estado. */
  private PersuasionEvidenceContract() {}

  /**
   * Exige cinco decisões distintas, sustentadas pelo contexto da mesma tarefa e sem números
   * inventados.
   */
  static void validate(JsonNode proof, Map<String, Object> task) {
    if (!"COMMERCIAL_PERSUASION_EVIDENCE_V1".equals(proof.path("contractVersion").asText())
        || !proof.path("criteria").isArray()
        || proof.path("criteria").size() != METRICS.size())
      throw new IllegalArgumentException("Íris deve comprovar os cinco pontos da comunicação.");
    JsonNode context;
    try {
      context =
          new ObjectMapper()
              .readTree(String.valueOf(task.getOrDefault("processContextJson", "{}")));
    } catch (IOException ex) {
      log.error(
          "Íris: contexto de provas inválido taskId={} sourceReference={}",
          task.get("taskId"),
          task.get("sourceReference"),
          ex);
      throw new IllegalArgumentException("Contexto das provas comerciais inválido.", ex);
    }
    var seen = new HashSet<String>();
    for (JsonNode criterion : proof.path("criteria")) {
      String code = criterion.path("criterion").asText();
      if (!METRICS.containsKey(code) || !seen.add(code))
        throw new IllegalArgumentException("Critério comercial desconhecido ou repetido: " + code);
      for (String field :
          List.of("decision", "sourcePointer", "sourceQuote", "metric", "limitation"))
        if (!criterion.path(field).isTextual() || criterion.path(field).asText().isBlank())
          throw new IllegalArgumentException("Decisão comercial sem " + field + ": " + code);
      if (!METRICS.get(code).contains(criterion.path("metric").asText()))
        throw new IllegalArgumentException(
            "Métrica incompatível com o critério comercial: " + code);
      if ("REPEAT_WITH_MARGIN".equals(code)) {
        if (!criterion.path("customerMessage").isNull())
          throw new IllegalArgumentException(
              "Margem orienta a decisão interna, não a mensagem da cliente.");
      } else if (!criterion.path("customerMessage").isTextual()
          || criterion.path("customerMessage").asText().isBlank())
        throw new IllegalArgumentException(
            "Critério sem mensagem concreta para a cliente: " + code);
      String pointer = criterion.path("sourcePointer").asText();
      if (SOURCES.stream().noneMatch(pointer::startsWith))
        throw new IllegalArgumentException("Fonte fora do contexto comercial autorizado: " + code);
      JsonNode source = context == null ? null : context.at(pointer);
      String quote = normalize(criterion.path("sourceQuote").asText());
      if (source == null
          || !source.isValueNode()
          || source.isNull()
          || !(source.isTextual()
              ? normalize(source.asText()).contains(quote)
              : source.asText().equals(quote)))
        throw new IllegalArgumentException("Citação comercial ausente da fonte da tarefa: " + code);
    }
  }

  /** Tolera somente espaços de formatação, preservando palavras e números da fonte. */
  private static String normalize(String value) {
    return value.replaceAll("\\s+", " ").trim();
  }
}
