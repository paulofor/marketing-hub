package com.marketinghub.product.modeling.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.productmodeling.ProductModelingRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Entrega fontes históricas à memória do sucessor, sem criar uma nova adoção de modelagem. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ModelingHistoricalResearch {
  private final ProductModelingRepository modelings;
  private final LearningSalesCycleRepository cycles;
  private final ObjectMapper json;

  /** Reutiliza somente fatos congelados antes do sucessor, com identidade e percurso limitados. */
  public JsonNode forCycle(LearningSalesCycle target, Long productId, Instant createdAt) {
    if (productId == null
        || !Objects.equals(target.getProductId(), productId)
        || createdAt == null
        || target.getCreatedAt() == null
        || createdAt.isBefore(target.getCreatedAt())) return json.nullNode();
    if (modelings.findByLearningCycleId(target.getId()).isPresent()) return json.nullNode();
    var visited = new HashSet<Long>();
    visited.add(target.getId());
    var cursor = target;
    for (int depth = 0; depth < 32 && cursor.getPreviousCycleId() != null; depth++) {
      if (!visited.add(cursor.getPreviousCycleId())) return json.nullNode();
      var parent = cycles.findById(cursor.getPreviousCycleId()).orElse(null);
      if (parent == null
          || !Objects.equals(parent.getProductId(), productId)
          || parent.getCreatedAt() == null
          || parent.getCreatedAt().isAfter(cursor.getCreatedAt())) return json.nullNode();
      var adopted = modelings.findByLearningCycleId(parent.getId());
      if (adopted.isPresent()) {
        var model = adopted.get();
        if (!Objects.equals(model.getProductId(), productId)
            || !Objects.equals(model.getExperimentId(), parent.getExperimentId())
            || model.getAdoptedAt() == null
            || model.getAdoptedAt().isAfter(target.getCreatedAt())) return json.nullNode();
        var frozen = read(model.getAdoptedContextJson(), target.getId(), parent.getId());
        if (!frozen.isObject()
            || !frozen.path("factualSources").isArray()
            || frozen.path("factualSources").isEmpty()) return json.nullNode();
        ObjectNode result = frozen.deepCopy();
        result.remove("strategicDecision");
        result.put("researchReuseOnly", true);
        result.put("historicalContextOnly", true);
        result.put("adoptionInherited", false);
        result.put("inheritedFromCycleId", parent.getId());
        result.put("inheritedFromExperimentId", parent.getExperimentId());
        result.put("targetCycleId", target.getId());
        result.put("commercialAuthorizationInherited", false);
        result.put("commercialValidationRequired", true);
        result.put(
            "guidance",
            result.path("guidance").asText()
                + " Este bloco é memória histórica, não substitui a adoção própria de modelagem quando aplicável. Conferir atualidade e limitações antes de reutilizar as fontes; não repetir pesquisa apenas por falta de contexto. A Big Idea atual, a hipótese e a implementação do sucessor orientam a nova avaliação. O estudo não transfere estratégia aprovada, pareceres, autorização financeira ou prova de demanda; as decisões deste ciclo continuam próprias.");
        return result;
      }
      cursor = parent;
    }
    return json.nullNode();
  }

  /** Lê a evidência congelada e preserva erro de integridade sem inventar contexto. */
  private JsonNode read(String value, Long targetCycleId, Long sourceCycleId) {
    try {
      if (value == null || value.isBlank())
        throw new IllegalArgumentException("Pesquisa histórica sem conteúdo.");
      return json.readTree(value);
    } catch (Exception ex) {
      log.error(
          "Modelagem: falha ao ler pesquisa histórica targetCycleId={} sourceCycleId={}",
          targetCycleId,
          sourceCycleId,
          ex);
      throw new IllegalStateException("Pesquisa histórica inválida.", ex);
    }
  }
}
