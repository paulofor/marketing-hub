package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleProcessContext.Work;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.util.Comparator;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Responsabilidade: localizar o trabalho restante do ciclo nas atividades oficiais da cadeia. */
@Component
public class LearningCycleWorkResolver {
  private final BusinessProcessChainDefinitionRepository chains;
  private final BusinessProcessActivityExecutionService executions;

  /** Resolve a projeção de atividades sob demanda, evitando recursão pela atividade chamadora. */
  public LearningCycleWorkResolver(
      BusinessProcessChainDefinitionRepository chains,
      @Lazy BusinessProcessActivityExecutionService executions) {
    this.chains = chains;
    this.executions = executions;
  }

  /** Avança a orientação depois de uma atividade concluída sem alterar etapas ou aprovações. */
  public Work resolve(LearningSalesCycle cycle) {
    if (!"OPEN".equals(cycle.getStatus())
        || !Set.of("PLANNING", "ADJUSTMENT", "VALIDATION").contains(cycle.getStage())) return null;
    var chain = chains.findById(cycle.getChainDefinitionId()).orElseThrow();
    String initialCode =
        "PLANNING".equals(cycle.getStage())
            ? "pde-commercial-plan-offer"
            : "pde-construction-approval";
    var initial =
        chain.getItems().stream()
            .filter(
                item ->
                    "ADJUSTMENT".equals(cycle.getStage()) && cycle.getReturnProcessId() != null
                        ? cycle.getReturnProcessId().equals(item.getProcessDefinition().getId())
                        : initialCode.equals(item.getProcessDefinition().getProcessCode()))
            .findFirst()
            .orElse(null);
    if (initial == null) return null;
    var allowed =
        "ADJUSTMENT".equals(cycle.getStage())
            ? Set.of(
                "pde-commercial-plan-offer",
                "pde-construction-approval",
                "pde-communication-sales-journey")
            : Set.of(initialCode);
    for (var item :
        chain.getItems().stream()
            .filter(value -> value.getSequenceNumber() >= initial.getSequenceNumber())
            .filter(value -> allowed.contains(value.getProcessDefinition().getProcessCode()))
            .sorted(Comparator.comparing(BusinessProcessChainItem::getSequenceNumber))
            .toList()) {
      var process = item.getProcessDefinition();
      var state =
          executions.productProcessExecutions(
              process.getId(), cycle.getProductId(), cycle.getId(), cycle.getChainDefinitionId());
      if (state.objectiveAchieved()) continue;
      var activity =
          state.activities().stream()
              .filter(value -> value.activityId().equals(state.currentActivityId()))
              .findFirst()
              .orElse(null);
      if (activity == null) return null;
      return new Work(
          process.getId(),
          item.getSequenceNumber(),
          process.getName(),
          activity.activityId(),
          activity.sequenceNumber(),
          activity.activityName(),
          activity.activityOwnerName(),
          activity.operationalState(),
          activity.stateReason(),
          "/products/"
              + cycle.getProductId()
              + "/value-chain-history/processes/"
              + process.getId()
              + "/activities?learningCycleId="
              + cycle.getId()
              + "&chainId="
              + cycle.getChainDefinitionId()
              + "#activity-"
              + activity.activityId());
    }
    return null;
  }
}
