package com.marketinghub.agenttask.service.pending;

import com.marketinghub.agenttask.service.pending.AgentTaskPendingWithOperatorGuidance.PreparationBudgetContext;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCyclePreparationBudget;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: entregar ao executor somente o recibo financeiro do próprio ciclo aberto. */
@Component
@RequiredArgsConstructor
public class AgentTaskPreparationBudgetGuidance {
  private final LearningSalesCycleRepository cycles;
  private final LearningCyclePreparationBudget preparation;

  /** Obtém a decisão estruturada, sem interpretar notas nem transferir teto de outro ciclo. */
  public PreparationBudgetContext read(Long productId, String sourceReference) {
    if (sourceReference == null || !sourceReference.matches("experiment:[1-9][0-9]{0,17}"))
      return null;
    var cycle =
        cycles
            .findByExperimentId(Long.valueOf(sourceReference.substring(11)))
            .filter(
                value ->
                    Objects.equals(value.getProductId(), productId)
                        && "OPEN".equals(value.getStatus()))
            .orElse(null);
    if (cycle == null) return null;
    var receipt = preparation.current(cycle);
    if (receipt == null) return null;
    return new PreparationBudgetContext(
        "PREPARATION_BUDGET_V1",
        productId,
        cycle.getId(),
        cycle.getExperimentId(),
        "USD",
        true,
        false,
        "/financial/execution?productId="
            + productId
            + "&chainId="
            + cycle.getChainDefinitionId()
            + "&cycleId="
            + cycle.getId(),
        receipt);
  }
}
