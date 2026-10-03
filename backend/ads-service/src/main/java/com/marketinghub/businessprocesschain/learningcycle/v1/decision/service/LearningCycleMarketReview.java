package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.StreamSupport;

/** Responsabilidade: validar a avaliação de mercado sem autorizar mudanças comerciais. */
final class LearningCycleMarketReview {
  private static final Set<String> SCOPES =
      Set.of("KEEP_FOCUS", "ADJACENT_SEGMENTS", "BROAD_PROBLEM");
  private static final Set<String> TEXT_FIELDS =
      Set.of(
          "currentAudience",
          "proposedAudience",
          "sharedProblem",
          "requiredAdaptations",
          "excludedAudiences",
          "evidenceLimits",
          "continueWhen",
          "adjustWhen",
          "stopWhen");

  /** Impede instanciação deste validador sem estado. */
  private LearningCycleMarketReview() {}

  /** Exige comparação completa, critério econômico e sucessor para redirecionar o mercado. */
  static void validate(JsonNode result, JsonNode context) {
    var review = result.path("marketReview");
    var fields = new HashSet<>(TEXT_FIELDS);
    fields.addAll(
        Set.of("recommendedScope", "deliveryReadiness", "primaryMetric", "requiresNewCycle"));
    require(
        review.isObject() && review.size() == fields.size(),
        "Avaliação de mercado obrigatória e completa.");
    review
        .fieldNames()
        .forEachRemaining(
            name -> require(fields.contains(name), "Campo de mercado fora do contrato: " + name));
    for (String field : TEXT_FIELDS)
      require(
          review.path(field).isTextual()
              && !review.path(field).asText().isBlank()
              && review.path(field).asText().length() <= 4000,
          "Avaliação de mercado incompleta: " + field);
    require(
        Set.of("SUPPORTED", "REQUIRES_ADAPTATION", "UNKNOWN")
            .contains(review.path("deliveryReadiness").asText()),
        "Capacidade de entrega inválida.");
    require(
        "NET_CONTRIBUTION_AFTER_ACQUISITION".equals(review.path("primaryMetric").asText()),
        "A revisão deve priorizar contribuição líquida após aquisição, não cliques.");
    require(
        review.path("requiresNewCycle").isBoolean(),
        "Declare a necessidade de novo ciclo e experimento.");
    Set<String> compared = new HashSet<>();
    for (JsonNode alternative : result.path("alternatives")) {
      require(alternative.size() == 6, "Alternativa de mercado contém campos fora do contrato.");
      String scope = alternative.path("marketScope").asText();
      require(
          SCOPES.contains(scope) && compared.add(scope),
          "Compare foco, segmentos adjacentes e dor ampla, sem repetição.");
    }
    require(compared.equals(SCOPES), "As três alternativas de mercado são obrigatórias.");
    if ("ADJUST".equals(result.path("action").asText()))
      require(
          review.path("requiresNewCycle").asBoolean(),
          "Todo ajuste exige novo ciclo e novo experimento.");
    String scope = review.path("recommendedScope").asText();
    require(
        SCOPES.contains(scope) || "INSUFFICIENT_EVIDENCE".equals(scope),
        "Recomendação de mercado inválida.");
    if (!"INSUFFICIENT_EVIDENCE".equals(scope))
      require(
          scope.equals(
              result
                  .path("alternatives")
                  .get(result.path("selectedAlternative").asInt())
                  .path("marketScope")
                  .asText()),
          "A recomendação de mercado deve coincidir com a alternativa escolhida.");
    if (Set.of("ADJACENT_SEGMENTS", "BROAD_PROBLEM").contains(scope)) {
      require(
          "ADJUST".equals(result.path("action").asText())
              && review.path("requiresNewCycle").asBoolean(),
          "Redirecionamento exige ajuste em novo ciclo e novo experimento.");
      require(
          StreamSupport.stream(context.path("returnTargets").spliterator(), false)
              .anyMatch(
                  target ->
                      "pde-commercial-plan-offer".equals(target.path("processCode").asText())
                          && "marketStrategy".equals(target.path("activityId").asText())
                          && target
                              .path("processDefinitionId")
                              .equals(result.path("returnProcessId"))
                          && target.path("activityId").equals(result.path("returnActivityId"))),
          "Redirecionamento deve retornar à estratégia de Atena na própria cadeia.");
    }
  }
}
