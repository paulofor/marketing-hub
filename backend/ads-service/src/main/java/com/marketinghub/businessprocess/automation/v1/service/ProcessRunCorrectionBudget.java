package com.marketinghub.businessprocess.automation.v1.service;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCyclePreparationBudget;
import com.marketinghub.repository.jdbc.learningcycle.CyclePreparationCostRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: conferir o teto existente antes de qualquer nova tarefa de agente no ciclo. */
@Component
@RequiredArgsConstructor
public class ProcessRunCorrectionBudget {
  private final LearningSalesCycleRepository cycles;
  private final LearningCyclePreparationBudget budgets;
  private final CyclePreparationCostRepository costs;

  /** Reutiliza recibos e custos do produto; nunca interpreta notas como autorização financeira. */
  public String blockReason(ProcessRun run, ProductProcessActivityExecutionGroupResponse activity) {
    if (run.getLearningCycleId() == null
        && (run.getSourceReference() == null
            || !run.getSourceReference().matches("experiment:[1-9][0-9]{0,17}"))) return null;
    var cycle =
        run.getLearningCycleId() == null
            ? cycles
                .findByExperimentId(Long.valueOf(run.getSourceReference().substring(11)))
                .orElse(null)
            : cycles.findById(run.getLearningCycleId()).orElse(null);
    if (cycle == null && run.getLearningCycleId() == null) return null;
    if (cycle == null
        || !Objects.equals(run.getProductId(), cycle.getProductId())
        || !Objects.equals(run.getChainDefinitionId(), cycle.getChainDefinitionId())
        || !Objects.equals(run.getSourceReference(), "experiment:" + cycle.getExperimentId())
        || !"OPEN".equals(cycle.getStatus()))
      return "A preparação exige um ciclo aberto e a autorização financeira do mesmo contexto.";
    var authorization = budgets.current(cycle);
    if (authorization == null)
      return "Preparação pausada: falta o teto acumulado de preparação neste produto e na linhagem do ciclo. "
          + "Use Ver demonstração financeira para conferir custos e registrar a decisão.";
    var slices = costs.summarize(run.getProductId());
    BigDecimal known =
        slices.stream()
            .map(slice -> slice.knownUsd())
            .filter(Objects::nonNull)
            .reduce(BigDecimal::add)
            .orElseGet(
                () ->
                    slices.stream().anyMatch(slice -> slice.unreportedTasks() > 0)
                        ? null
                        : BigDecimal.ZERO);
    if (known == null)
      return "Preparação pausada: o custo acumulado ainda não foi informado. Confira as lacunas na demonstração financeira antes de iniciar outra tarefa.";
    BigDecimal available =
        authorization.totalLimitUsd().subtract(known).subtract(authorization.reserveUsd());
    BigDecimal previous =
        activity.tasks().stream()
            .filter(
                task ->
                    Objects.equals(run.getSourceReference(), task.sourceReference())
                        && Objects.equals(run.getProcessDefinitionId(), task.processDefinitionId()))
            .map(task -> task.estimatedCostUsd())
            .filter(Objects::nonNull)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    if (available.signum() <= 0 || available.compareTo(previous) < 0)
      return "Atenção: o saldo estimado do teto acumulado, após custos anteriores e reserva, não cobre outra tarefa desta atividade. "
          + "Confira e ajuste o teto em Ver demonstração financeira. A estimativa histórica não é garantia do custo futuro.";
    return null;
  }
}
