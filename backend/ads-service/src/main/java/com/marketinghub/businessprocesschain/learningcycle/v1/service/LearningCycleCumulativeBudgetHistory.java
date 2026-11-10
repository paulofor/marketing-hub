package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: localizar recibos financeiros originais na linhagem do próprio produto. */
@Component
@RequiredArgsConstructor
public class LearningCycleCumulativeBudgetHistory {
  private final LearningSalesCycleRepository cycles;
  private final LearningSalesCycleEventRepository events;
  private final LearningCycleJson json;

  /** Responsabilidade: preservar o evento original distinguindo histórico e teto elegível. */
  public record Receipt(LearningSalesCycleEvent event, boolean eligible) {}

  /** Recupera decisões em ordem global, sem copiar recibos nem consultar ciclos descendentes. */
  public List<Receipt> history(LearningSalesCycle cycle, String action) {
    Map<Long, LearningSalesCycle> lineage = new LinkedHashMap<>();
    var source = cycle;
    while (source != null) {
      require(
          Objects.equals(source.getProductId(), cycle.getProductId())
              && Objects.equals(source.getChainCode(), cycle.getChainCode())
              && !lineage.containsKey(source.getId()),
          "A linhagem financeira possui produto, cadeia ou vínculo inválido. Confira o histórico.");
      lineage.put(source.getId(), source);
      if (source.getPreviousCycleId() == null) break;
      source =
          cycles
              .findById(source.getPreviousCycleId())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "O ciclo predecessor não foi encontrado para conferir o teto acumulado."));
    }
    return events
        .findByCycleIdInAndActionOrderByCreatedAtDescIdDesc(lineage.keySet(), action)
        .stream()
        .filter(event -> lineage.containsKey(event.getCycleId()))
        .filter(
            event ->
                cycle.getClosedAt() == null
                    || event.getCreatedAt() != null
                        && !event.getCreatedAt().isAfter(cycle.getClosedAt()))
        .map(event -> new Receipt(event, eligible(event, lineage.get(event.getCycleId()))))
        .toList();
  }

  /** Confere a identidade original sem apagar o histórico de versões invalidadas. */
  private boolean eligible(LearningSalesCycleEvent event, LearningSalesCycle source) {
    if (source == null || event.getCreatedAt() == null) return false;
    var proof = json.read(event.getEvidenceJson());
    return (source.getVersionChangedAt() == null
            || !event.getCreatedAt().isBefore(source.getVersionChangedAt()))
        && Objects.equals(source.getProductVersion(), proof.path("productVersion").asText())
        && Objects.equals(source.getExperimentId(), proof.path("experimentId").asLong())
        && Objects.equals(source.getChainDefinitionId(), proof.path("chainDefinitionId").asLong())
        && (!proof.has("productId")
            || Objects.equals(source.getProductId(), proof.path("productId").asLong()));
  }
}
