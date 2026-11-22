package com.marketinghub.businessprocess.automation.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunUserAction;
import com.marketinghub.repository.jdbc.learningcycle.CyclePreparationCostRepository;
import com.marketinghub.repository.jpa.processautomation.ProcessRunEventRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: explicar e proteger uma pausa financeira explicitamente registrada pelo
 * operador.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessRunPauseGuidance {
  private final ProcessRunEventRepository events;
  private final CyclePreparationCostRepository costs;
  private final ObjectMapper json;

  /**
   * Apresenta a justificativa humana registrada, inclusive em pausas comuns sem decisão financeira.
   */
  public String reason(ProcessRun run) {
    if (run.getId() == null || !Set.of("PAUSED", "PAUSING").contains(run.getStatus())) return null;
    return events
        .findFirstByRunIdAndEventTypeOrderByIdDesc(run.getId(), "PAUSE_CONTEXT")
        .map(event -> "Processo pausado: " + event.getMessage())
        .orElse(null);
  }

  /** Protege a pausa enquanto houver custos ausentes sem dispensa explícita por tarefa. */
  public ProcessRunUserAction resolve(ProcessRun run) {
    if (run.getId() == null || !Set.of("PAUSED", "PAUSING").contains(run.getStatus())) return null;
    var event = events.findFirstByRunIdAndEventTypeOrderByIdDesc(run.getId(), "PAUSE_CONTEXT");
    if (event.isEmpty()) return null;
    try {
      var detail = json.readTree(event.get().getDetailsJson());
      if (!"COST_RECONCILIATION".equals(detail.path("reasonCode").asText())) return null;
      long missing =
          costs.summarize(run.getProductId()).stream()
                  .mapToLong(slice -> slice.unreportedTasks())
                  .sum()
              - costs.waivedUnknownTasks(run.getProductId());
      if (missing <= 0) return null;
      return new ProcessRunUserAction(
          "RECONCILE_COSTS_BEFORE_RESUME",
          "Confira os custos antes de retomar",
          "O processo foi pausado para conferir os gastos. Há "
              + missing
              + " tarefas antigas sem custo informado; por isso o saldo disponível ainda não é conhecido. "
              + detail.path("reason").asText(""),
          "Você · decisão financeira; Plutus · conferência dos custos",
          "Ver custos pendentes",
          run.getLearningCycleId() == null
              ? "/financial/plans?productId=" + run.getProductId()
              : "/financial/execution?productId="
                  + run.getProductId()
                  + "&chainId="
                  + run.getChainDefinitionId()
                  + "&cycleId="
                  + run.getLearningCycleId(),
          "Após conciliar ou dispensar explicitamente as tarefas selecionadas, retome o processo em ação separada. O responsável pela atividade fará a revisão pendente, preservando as aprovações anteriores. Alterar o teto não resolve custos ausentes.",
          "internal://process-runs/" + run.getId() + "/events/" + event.get().getId());
    } catch (Exception ex) {
      log.error(
          "Falha ao consultar motivo de pausa. runId={} productId={}",
          run.getId(),
          run.getProductId(),
          ex);
      throw new IllegalStateException(
          "Não foi possível conferir o motivo da pausa. Atualize antes de retomar.", ex);
    }
  }
}
