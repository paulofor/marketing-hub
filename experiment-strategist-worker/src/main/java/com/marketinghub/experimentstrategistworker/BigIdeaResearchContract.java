package com.marketinghub.experimentstrategistworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/** Valida identidade, completude e fronteira de demanda da proposta isolada de Atena. */
final class BigIdeaResearchContract {
  static final String MODE = "PRODUCT_BIG_IDEA_RESEARCH";

  /** Impede resposta de outro produto e proposta sem fontes ou limites rastreáveis. */
  static void validate(JsonNode result, JsonNode input) {
    if (!"PRODUCT_BIG_IDEA_RESEARCH_V1".equals(result.path("contractVersion").asText())
        || !result.path("productId").isIntegralNumber()
        || result.path("productId").asLong()
            != input.path("bigIdeaResearch").path("productId").asLong()
        || !"HYPOTHESIS".equals(result.path("demandAssessment").path("status").asText())
        || !result.path("alternatives").isArray()
        || result.path("alternatives").size() != 3
        || !result.path("sources").isArray()
        || result.path("sources").size() < 2
        || result.path("sources").size() > 8)
      throw new IllegalArgumentException("Identidade ou contrato de demanda da Big Idea inválido.");
    Map<String, Integer> limits =
        Map.of(
            "centralIdea",
            500,
            "audience",
            1500,
            "desire",
            1500,
            "promise",
            1500,
            "mechanism",
            2000,
            "evidence",
            3000,
            "limits",
            2000);
    limits.forEach(
        (field, max) -> {
          JsonNode value = result.path("bigIdea").path(field);
          if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > max)
            throw new IllegalArgumentException("Big Idea incompleta: " + field);
        });
    requireText(result, "selectionReason", 1600);
    for (String field :
        List.of(
            "frequencyAndUrgency",
            "willingnessToPaySignals",
            "evidenceAgainst",
            "gaps",
            "validationPlan")) requireText(result.path("demandAssessment"), field, 1600);
    for (JsonNode option : result.path("alternatives"))
      for (String field : List.of("idea", "benefit", "risk", "effort", "fit"))
        requireText(option, field, 900);
    var classes = new HashSet<String>();
    for (JsonNode source : result.path("sources")) {
      for (String field : List.of("url", "title", "accessedAt", "evidenceClass", "learning"))
        requireText(source, field, 1000);
      if (!source.path("url").asText().matches("https?://[^\\s]+")
          || source.path("evidenceClass").asText().isBlank()
          || source.path("learning").asText().isBlank())
        throw new IllegalArgumentException("Fonte de demanda incompleta.");
      classes.add(source.path("evidenceClass").asText());
    }
    if (classes.size() < 2)
      throw new IllegalArgumentException("Pesquisa precisa de duas classes de evidência.");
  }

  /** Exige texto real e limitado, sem coerção de tipos ou campos vazios. */
  private static void requireText(JsonNode object, String field, int max) {
    JsonNode value = object.path(field);
    if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > max)
      throw new IllegalArgumentException("Campo da pesquisa incompleto: " + field);
  }
}
