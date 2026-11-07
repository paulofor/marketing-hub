package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.validationscope.v1.PdeValidationScopePolicy;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/** Responsabilidade: delimitar a homologação às capacidades declaradas da candidata exata. */
@Slf4j
public final class LearningCycleValidationProtocol {
  /** Impede instâncias de uma política sem estado. */
  private LearningCycleValidationProtocol() {}

  /** Entrega o protocolo comum e inclui comparação somente com capacidade da mesma versão. */
  public static ObjectNode resolve(ObjectMapper json, LearningSalesCycle cycle, JsonNode input) {
    try (var common =
        LearningCycleValidationProtocol.class.getResourceAsStream(
            "/contracts/pde-agent-validation-plan-v1.json")) {
      if (common == null) throw new IOException("Protocolo multiagente ausente.");
      ObjectNode result = (ObjectNode) json.readTree(common);
      result.put("scopeContractVersion", "PDE_VALIDATION_SCOPE_V1");
      result.put("productId", cycle.getProductId());
      result.put("cycleId", cycle.getId());
      result.put("experimentId", cycle.getExperimentId());
      result.put("prototypeVersion", cycle.getProductVersion());
      boolean comparison =
          input != null
              && input.isObject()
              && "PDE_IMPLEMENTED_INPUT_V1".equals(input.path("contractVersion").asText())
              && PdeValidationScopePolicy.sameCandidate(input, result)
              && "PDE_DOCUMENTED_INPUT_COMPARISON_V3"
                  .equals(input.path("validationFixtureContract").asText());
      result.put("inputComparisonApplicable", comparison);
      if (comparison) {
        try (var profile =
            LearningCycleValidationProtocol.class.getResourceAsStream(
                "/contracts/pde-documented-input-comparison-v3.json")) {
          if (profile == null) throw new IOException("Perfil comparativo ausente.");
          result.set("documentedInputComparison", json.readTree(profile));
        }
      }
      return result;
    } catch (IOException ex) {
      log.error(
          "Falha ao resolver protocolo productId={} cycleId={} version={}",
          cycle.getProductId(),
          cycle.getId(),
          cycle.getProductVersion(),
          ex);
      throw new IllegalStateException("Protocolo multiagente indisponível.", ex);
    }
  }

  /** Aplica a mesma política do executor, sem conceder capacidade por menção textual. */
  public static boolean hasUnscopedComparison(JsonNode result, JsonNode protocol) {
    return PdeValidationScopePolicy.violation(result, protocol).isPresent();
  }
}
