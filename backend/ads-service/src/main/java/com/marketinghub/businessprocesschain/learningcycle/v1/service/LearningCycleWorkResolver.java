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
  private final LearningCycleConstructionPrerequisite constructionPrerequisite;

  /** Resolve a projeção de atividades sob demanda, evitando recursão pela atividade chamadora. */
  public LearningCycleWorkResolver(
      BusinessProcessChainDefinitionRepository chains,
      @Lazy BusinessProcessActivityExecutionService executions,
      LearningCycleConstructionPrerequisite constructionPrerequisite) {
    this.chains = chains;
    this.executions = executions;
    this.constructionPrerequisite = constructionPrerequisite;
  }

  /**
   * Responsabilidade: distinguir trabalho pendente, conclusão comprovada e ausência de contexto.
   */
  public record Resolution(Work nextWork, boolean completed) {}

  /** Mantém o contrato de navegação para consumidores que precisam apenas do próximo trabalho. */
  public Work resolve(LearningSalesCycle cycle) {
    return resolvePreparation(cycle).nextWork();
  }

  /**
   * Confere trabalhos delegados também na recuperação do formato, sem aprovar etapas comerciais.
   */
  public Resolution resolvePreparation(LearningSalesCycle cycle) {
    return resolvePreparation(cycle, false);
  }

  /** Reconfere a comunicação completa, inclusive seu aceite, antes da homologação sem vídeos. */
  public Resolution resolveStaticPreparation(LearningSalesCycle cycle) {
    return resolvePreparation(cycle, true);
  }

  /**
   * Consulta o estado compacto e exige construção homologada mesmo no retorno direto à comunicação.
   */
  private Resolution resolvePreparation(LearningSalesCycle cycle, boolean includeCommunication) {
    if (!"OPEN".equals(cycle.getStatus())
        || !Set.of("PLANNING", "ADJUSTMENT", "VIDEO_BRIEF", "VALIDATION")
            .contains(cycle.getStage())) return new Resolution(null, false);
    boolean adjustment =
        Set.of("ADJUSTMENT", "VIDEO_BRIEF").contains(cycle.getStage())
            || includeCommunication && "VALIDATION".equals(cycle.getStage());
    var chain = chains.findById(cycle.getChainDefinitionId()).orElseThrow();
    String initialCode =
        "PLANNING".equals(cycle.getStage())
            ? "pde-commercial-plan-offer"
            : "pde-construction-approval";
    var initial =
        chain.getItems().stream()
            .filter(
                item ->
                    adjustment && cycle.getReturnProcessId() != null
                        ? cycle.getReturnProcessId().equals(item.getProcessDefinition().getId())
                        : initialCode.equals(item.getProcessDefinition().getProcessCode()))
            .findFirst()
            .orElse(null);
    if (initial == null) return new Resolution(null, false);
    var allowed =
        adjustment
            ? Set.of(
                "pde-commercial-plan-offer",
                "pde-construction-approval",
                "pde-communication-sales-journey")
            : Set.of(initialCode);
    var delegated =
        chain.getItems().stream()
            .filter(
                value ->
                    value.getSequenceNumber() >= initial.getSequenceNumber()
                        || adjustment
                            && "pde-construction-approval"
                                .equals(value.getProcessDefinition().getProcessCode()))
            .filter(value -> allowed.contains(value.getProcessDefinition().getProcessCode()))
            .sorted(Comparator.comparing(BusinessProcessChainItem::getSequenceNumber))
            .toList();
    if (delegated.isEmpty()) return new Resolution(null, false);
    for (var item : delegated) {
      var process = item.getProcessDefinition();
      var state =
          executions.productProcessExecutions(
              process.getId(),
              cycle.getProductId(),
              cycle.getId(),
              cycle.getChainDefinitionId(),
              false);
      var pending =
          state.objectiveAchieved() && "pde-construction-approval".equals(process.getProcessCode())
              ? constructionPrerequisite.pending(cycle)
              : null;
      if (state.objectiveAchieved() && pending == null) continue;
      var activity =
          state.activities().stream()
              .filter(
                  value ->
                      value
                          .activityId()
                          .equals(
                              pending == null ? state.currentActivityId() : "agentValidationGate"))
              .findFirst()
              .orElse(null);
      if (activity == null) return new Resolution(null, false);
      return new Resolution(
          new Work(
              process.getId(),
              item.getSequenceNumber(),
              process.getName(),
              activity.activityId(),
              activity.sequenceNumber(),
              activity.activityName(),
              activity.activityOwnerName(),
              pending == null ? activity.operationalState() : "BLOCKED",
              pending == null ? activity.stateReason() : pending.reason(),
              "/products/"
                  + cycle.getProductId()
                  + "/value-chain-history/processes/"
                  + process.getId()
                  + "/activities?learningCycleId="
                  + cycle.getId()
                  + "&chainId="
                  + cycle.getChainDefinitionId()
                  + "#activity-"
                  + activity.activityId()),
          false);
    }
    return new Resolution(null, true);
  }
}
