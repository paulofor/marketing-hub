package com.marketinghub.landinggeneratoragent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Responsabilidade: reduzir o snapshot do Dédalo ao contexto necessário em cada interação. */
final class LandingPromptContextReducer {
  private final ObjectMapper objectMapper;

  /** Inicializa o redutor com o serializador usado pelo contrato do worker. */
  LandingPromptContextReducer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Remove HTML integral e auditoria bruta da decisão, preservando achados objetivos da revisão.
   */
  Map<String, Object> forPlanning(Map<String, Object> context) {
    Map<String, Object> reduced = baseWithoutRawQualityReview(context);
    reduced.remove("landingHtml");
    return Collections.unmodifiableMap(reduced);
  }

  /** Entrega o HTML atual apenas à materialização e mantém a revisão em formato compacto. */
  Map<String, Object> forMaterialization(Map<String, Object> context) {
    return Collections.unmodifiableMap(baseWithoutRawQualityReview(context));
  }

  /** Copia o snapshot e substitui o Quality Review bruto por um resumo contratual. */
  private Map<String, Object> baseWithoutRawQualityReview(Map<String, Object> context) {
    Map<String, Object> reduced = new LinkedHashMap<>(context);
    Object rawQualityReview = reduced.remove("qualityReview");
    Map<String, Object> summary = compactQualityReview(rawQualityReview);
    if (!summary.isEmpty()) {
      reduced.put("qualityReviewSummary", summary);
    }
    return reduced;
  }

  /** Seleciona somente score, decisão, critérios e causas necessários à correção. */
  private Map<String, Object> compactQualityReview(Object rawQualityReview) {
    if (rawQualityReview == null) {
      return Map.of();
    }
    JsonNode review = objectMapper.valueToTree(rawQualityReview);
    Map<String, Object> summary = new LinkedHashMap<>();
    copy(review, "score", "baselineQualityReviewScore", summary);
    copy(review, "approvalRecommendation", "approvalRecommendation", summary);
    copy(review, "targetAudienceSpecificity", "targetAudienceSpecificity", summary);
    copy(review, "commercialReadiness", "commercialReadiness", summary);
    copy(review, "criteriaScores", "criteriaScores", summary);
    copy(review, "blockingIssues", "blockingIssues", summary);
    copy(review, "recommendedRegeneration", "recommendedRegeneration", summary);
    copy(review, "acceptanceCriteria", "acceptanceCriteria", summary);
    return summary;
  }

  /** Copia um campo presente sem transportar nós de auditoria não contratados. */
  private void copy(
      JsonNode source, String sourceName, String targetName, Map<String, Object> target) {
    JsonNode value = source.path(sourceName);
    if (!value.isMissingNode() && !value.isNull()) {
      target.put(targetName, objectMapper.convertValue(value, Object.class));
    }
  }
}
