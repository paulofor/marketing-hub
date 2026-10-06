package com.marketinghub.businessprocess.automation.v1.service;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import com.marketinghub.experimentstrategist.service.CommercialAssumptionVersionCompatibility;
import com.marketinghub.financialagent.service.FinancialAgentService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: recuperar a passagem perdida de uma proposta concluída na preparação ativa. */
@Component
@RequiredArgsConstructor
public class ProcessRunAssumptionRecovery {
  private final CommercialPlanRepository plans;
  private final ExperimentRepository experiments;
  private final ExperimentStrategistExecutionRepository proposals;
  private final FinancialAgentExecutionRepository financial;
  private final CommercialPlanVersionService versions;
  private final FinancialAgentService plutus;

  /** Correlaciona a passagem recuperada sem afirmar aprovação nem conclusão da comunicação. */
  public record Recovered(Long proposalId, Long financialExecutionId, Long taskId) {}

  /**
   * Reutiliza exclusivamente uma proposta da execução ativa; parecer existente, inclusive
   * rejeitado, nunca autoriza nova inferência ou substituição do histórico.
   */
  public Optional<Recovered> recover(ProcessRun run, String processCode) {
    if (!"pde-communication-sales-journey".equals(processCode)
        || !"communicationContract".equals(run.getCurrentActivityId())
        || !Set.of("QUEUED", "RUNNING", "WAITING_INPUT", "WAITING_ACTIVITY")
            .contains(run.getStatus())
        || run.getCreatedAt() == null
        || run.getSourceReference() == null
        || !run.getSourceReference().matches("experiment:[1-9][0-9]*")) return Optional.empty();
    var experiment =
        experiments.findById(Long.valueOf(run.getSourceReference().substring(11))).orElse(null);
    if (experiment == null
        || experiment.getProduct() == null
        || !Objects.equals(run.getProductId(), experiment.getProduct().getId())
        || Boolean.FALSE.equals(experiment.getProduct().getAutomaticExecutionEnabled())
        || !"PLANNED".equals(String.valueOf(experiment.getStatus()))) return Optional.empty();
    var candidates = plans.findByExperimentReference(experiment.getId());
    if (candidates.size() != 1) return Optional.empty();
    var plan = candidates.getFirst();
    var proposal =
        proposals
            .findFirstByCommercialPlanIdAndAuthorityModeOrderByCreatedAtDescIdDesc(
                plan.getId(), "COMMERCIAL_ASSUMPTIONS_PROPOSAL")
            .orElse(null);
    if (proposal == null
        || proposal.getCommercialPlan() == null
        || !Objects.equals(plan.getId(), proposal.getCommercialPlan().getId())
        || proposal.getStatus() != ExperimentStrategistExecutionStatus.COMPLETED
        || proposal.getCreatedAt() == null
        || proposal.getCreatedAt().isBefore(run.getCreatedAt())
        || proposal.getRecommendationJson() == null
        || proposal.getRecommendationJson().isBlank()
        || !CommercialAssumptionVersionCompatibility.matches(
            proposal, versions.current(plan.getId()))
        || financial.findByStrategistExecutionId(proposal.getId()).isPresent())
      return Optional.empty();
    var recovered =
        plutus.startAssumptionValidation(
            plan.getId(), proposal.getId(), proposal.getRecommendationJson());
    return Optional.of(new Recovered(proposal.getId(), recovered.id(), recovered.agentTaskId()));
  }
}
