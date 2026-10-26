package com.marketinghub.communicationagentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Confere a proveniência e a ligação emocional das decisões sem substituir o julgamento de Psique.
 */
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
   * Exige cinco decisões da mesma tarefa e valida a conexão emocional de V2, preservando recibos
   * V1.
   */
  static void validate(JsonNode proof, Map<String, Object> task) {
    String version = proof.path("contractVersion").asText();
    if (!List.of("COMMERCIAL_PERSUASION_EVIDENCE_V1", "COMMERCIAL_PERSUASION_EVIDENCE_V2")
            .contains(version)
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
      validateSource(context, pointer, criterion.path("sourceQuote").asText(), code);
    }
    if ("COMMERCIAL_PERSUASION_EVIDENCE_V2".equals(version))
      validateEmotionalConnection(proof, context);
    else if (proof.has("emotionalConnection"))
      throw new IllegalArgumentException("Conexão emocional exige versão V2 explícita.");
  }

  /** Exige a ligação emocional nas novas inferências sem invalidar recuperações históricas. */
  static void validateFresh(JsonNode proof, Map<String, Object> task) {
    validate(proof, task);
    if (!"COMMERCIAL_PERSUASION_EVIDENCE_V2".equals(proof.path("contractVersion").asText()))
      throw new IllegalArgumentException("Nova comunicação exige conexão emocional V2.");
  }

  /** Confere direção, hipótese de crença e mensagens efetivas, sem declarar emoção comprovada. */
  private static void validateEmotionalConnection(JsonNode proof, JsonNode context) {
    JsonNode connection = proof.path("emotionalConnection");
    for (String field :
        List.of(
            "everydaySituation",
            "beliefHypothesis",
            "recognizedDesire",
            "emotionalPayoff",
            "reframe",
            "directionSourcePointer",
            "directionSourceQuote",
            "customerOpening",
            "proofBridge",
            "easyNextStep",
            "limitation"))
      if (!connection.path(field).isTextual() || connection.path(field).asText().isBlank())
        throw new IllegalArgumentException("Conexão emocional sem " + field + ".");
    String pointer = connection.path("directionSourcePointer").asText();
    if (!pointer.startsWith("/productBigIdea/") && !pointer.startsWith("/marketStrategicContract/"))
      throw new IllegalArgumentException(
          "Conexão emocional sem direção da Big Idea ou estratégia.");
    validateSource(
        context, pointer, connection.path("directionSourceQuote").asText(), "direção emocional");
    Map<String, String> messages =
        Map.of(
            "RECOGNIZED_DESIRE", "customerOpening",
            "VALUE_BEFORE_COMMITMENT", "proofBridge",
            "EASY_FIRST_STEP", "easyNextStep");
    for (JsonNode criterion : proof.path("criteria")) {
      String field = messages.get(criterion.path("criterion").asText());
      if (field != null
          && !normalize(connection.path(field).asText())
              .equals(normalize(criterion.path("customerMessage").asText())))
        throw new IllegalArgumentException("Mensagem diverge da conexão emocional: " + field);
    }
  }

  /** Confirma a citação escalar na entrada autorizada, sem transformá-la em prova de demanda. */
  private static void validateSource(
      JsonNode context, String pointer, String rawQuote, String label) {
    if (SOURCES.stream().noneMatch(pointer::startsWith))
      throw new IllegalArgumentException("Fonte fora do contexto comercial autorizado: " + label);
    JsonNode source = context == null ? null : context.at(pointer);
    String quote = normalize(rawQuote);
    if (quote.isBlank()
        || source == null
        || !source.isValueNode()
        || source.isNull()
        || !(source.isTextual()
            ? normalize(source.asText()).contains(quote)
            : source.asText().equals(quote)))
      throw new IllegalArgumentException("Citação comercial ausente da fonte da tarefa: " + label);
  }

  /** Tolera somente espaços de formatação, preservando palavras e números da fonte. */
  private static String normalize(String value) {
    return value.replaceAll("\\s+", " ").trim();
  }
}
