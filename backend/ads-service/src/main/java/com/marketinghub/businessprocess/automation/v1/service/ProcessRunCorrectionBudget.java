package com.marketinghub.businessprocess.automation.v1.service;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunFinancialPending;
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

  /** Reutiliza a mesma decisão estruturada na proteção de disparo, consulta e retomada. */
  public String blockReason(ProcessRun run, ProductProcessActivityExecutionGroupResponse activity) {
    var pending = inspect(run, activity);
    return pending == null ? null : pending.reason();
  }

  /** Consulta recibos e custos reais, distinguindo falta de aprovação de falta de cobertura. */
  public ProcessRunFinancialPending inspect(
      ProcessRun run, ProductProcessActivityExecutionGroupResponse activity) {
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
      return pending(
          activity,
          null,
          "CONTEXT_UNAVAILABLE",
          "Não foi possível validar a aprovação deste contexto",
          "A preparação exige um ciclo aberto e a autorização financeira do mesmo contexto.",
          "Equipe responsável pelo processo",
          null);
    var authorization = budgets.current(cycle);
    String financeUrl =
        "/financial/execution?productId="
            + cycle.getProductId()
            + "&chainId="
            + cycle.getChainDefinitionId()
            + "&cycleId="
            + cycle.getId()
            + "#ceiling-AGENTS";
    if (authorization == null) {
      boolean outdated = !budgets.history(cycle).isEmpty();
      return pending(
          activity,
          cycle.getProductVersion(),
          outdated ? "APPROVAL_OUTDATED" : "APPROVAL_REQUIRED",
          outdated
              ? "A aprovação de preparação está desatualizada"
              : "Falta aprovar o limite de preparação deste ciclo",
          outdated
              ? "A aprovação anterior não vale para o contexto e a versão atuais. Atualize a decisão na demonstração financeira para continuar."
              : "Ainda falta o teto de preparação aprovado para este ciclo e versão. Ajuste na demonstração financeira para continuar. A falta de aprovação não comprova falta de saldo.",
          "Você · responsável pelo limite de preparação",
          financeUrl);
    }
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
      return pending(
          activity,
          cycle.getProductVersion(),
          "COST_UNKNOWN",
          "Falta conferir o custo de preparação",
          "O custo acumulado ainda não foi informado. Confira as lacunas na demonstração financeira antes de iniciar outra tarefa.",
          "Equipe responsável pelo registro dos custos",
          financeUrl);
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
      return pending(
          activity,
          cycle.getProductVersion(),
          "LIMIT_INSUFFICIENT",
          "O limite aprovado não cobre a próxima tarefa",
          "O saldo estimado, após custos anteriores e reserva, não cobre outra tarefa desta atividade. Confira e ajuste o teto na demonstração financeira. A estimativa histórica não é garantia do custo futuro.",
          "Você · responsável pelo limite de preparação",
          financeUrl);
    return null;
  }

  /** Expõe ação contextual sem fabricar parecer, alterar recibos ou retomar automaticamente. */
  private ProcessRunFinancialPending pending(
      ProductProcessActivityExecutionGroupResponse activity,
      String version,
      String code,
      String title,
      String reason,
      String responsible,
      String url) {
    return new ProcessRunFinancialPending(
        code,
        activity.activityId(),
        version,
        title,
        reason,
        responsible,
        "COST_UNKNOWN".equals(code)
            ? "Conferir custos de preparação"
            : "Ajustar limite de preparação",
        url,
        "COST_UNKNOWN".equals(code)
            ? "A equipe precisa completar os custos. Ao voltar, o servidor revalida a cobertura e os demais requisitos; nenhum valor é aprovado automaticamente."
            : "CONTEXT_UNAVAILABLE".equals(code)
                ? "Reabra a atividade pelo ciclo correto. A equipe precisa conferir o vínculo antes de qualquer aprovação."
                : "Salve e aprove a decisão na demonstração financeira. Ao voltar, o servidor confere este ciclo e versão e os demais requisitos. A aprovação não retoma o processo automaticamente.");
  }
}
