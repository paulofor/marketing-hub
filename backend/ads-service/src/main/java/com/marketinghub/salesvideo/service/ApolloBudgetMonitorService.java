package com.marketinghub.salesvideo.service;

import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.VideoProductionCycle;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: consolidar consumo de Apolo e bloquear ciclos que excedam o teto aprovado. */
@Component
public class ApolloBudgetMonitorService {
  private static final String APOLLO_BLOCKED = "APOLLO_BLOCKED";
  private final VideoProductionCycleRepository cycleRepository;
  private final StudioCostLedgerService ledgerService;
  private final SalesVideoJobRepository jobRepository;

  /** Inicializa o monitor com as fontes canônicas de ciclo e consumo por task. */
  public ApolloBudgetMonitorService(
      VideoProductionCycleRepository cycleRepository,
      StudioCostLedgerService ledgerService,
      SalesVideoJobRepository jobRepository) {
    this.cycleRepository = cycleRepository;
    this.ledgerService = ledgerService;
    this.jobRepository = jobRepository;
  }

  /** Recalcula o ciclo após cada task aceita ou liquidada e persiste o alerta operacional. */
  @Transactional
  public void reconcile(Long cycleId, Long jobId, String providerTaskId) {
    if (cycleId == null) return;
    cycleRepository
        .findById(cycleId)
        .ifPresent(cycle -> reconcileCycle(cycle, jobId, providerTaskId, Instant.now()));
  }

  /** Marca saldo insuficiente imediatamente para impedir qualquer nova geração do ciclo. */
  @Transactional
  public void blockForInsufficientCredits(Long cycleId, Long jobId, String detail) {
    if (cycleId == null) return;
    cycleRepository
        .findById(cycleId)
        .ifPresent(
            cycle -> {
              cycle.setStatus(APOLLO_BLOCKED);
              cycle.setBudgetMonitorStatus("BLOCKED");
              cycle.setBudgetAlertCode("INSUFFICIENT_CREDITS");
              cycle.setBudgetAlertDetail(
                  "Apolo interrompido após saldo insuficiente no job #"
                      + jobId
                      + ". Nenhuma nova task pode ser aberta. "
                      + safeDetail(detail));
              cycle.setBudgetAlertAt(Instant.now());
              cycle.setUpdatedAt(Instant.now());
              cycleRepository.save(cycle);
            });
  }

  /** Aplica a fotografia financeira idempotente e decide se o teto foi violado. */
  private void reconcileCycle(
      VideoProductionCycle cycle, Long jobId, String providerTaskId, Instant observedAt) {
    java.util.Map<String, Object> consumption =
        ledgerService.cycleProviderConsumption(cycle.getId());
    long taskCount = ((Number) consumption.get("taskCount")).longValue();
    Long credits = ((Number) consumption.get("credits")).longValue();
    BigDecimal taskCost = (BigDecimal) consumption.get("costUsd");
    BigDecimal ledgerCost = ledgerService.cycleKnownLedgerCostUsd(cycle.getId());
    BigDecimal cost = greatest(taskCost, ledgerCost);
    cycle.setMonitoredTaskCount(taskCount);
    cycle.setMonitoredCredits(credits == null ? 0L : credits);
    cycle.setKnownCostUsd(cost == null ? BigDecimal.ZERO : cost);
    boolean exceeded =
        cycle.getBudgetLimitUsd() != null
            && cost != null
            && cost.compareTo(cycle.getBudgetLimitUsd()) > 0;
    if (exceeded) {
      cycle.setStatus(APOLLO_BLOCKED);
      cycle.setBudgetMonitorStatus("BLOCKED");
      cycle.setBudgetAlertCode("BUDGET_EXCEEDED");
      cycle.setBudgetAlertDetail(
          "Apolo interrompido: custo monitorado de US$ "
              + cost
              + " excedeu o teto de US$ "
              + cycle.getBudgetLimitUsd()
              + ".");
      jobRepository
          .findById(jobId)
          .ifPresent(
              job -> {
                job.setStatus(SalesVideoStatus.VIDEO_FAILED);
                job.setFailureCode("APOLLO_BUDGET_EXCEEDED");
                job.setFailureDetail(cycle.getBudgetAlertDetail());
                job.setFinishedAt(observedAt);
                jobRepository.save(job);
              });
    } else {
      cycle.setBudgetMonitorStatus("WATCHING");
      cycle.setBudgetAlertCode("NEW_PROVIDER_TASK");
      cycle.setBudgetAlertDetail(
          "Nova task "
              + providerTaskId
              + " detectada. Total: "
              + taskCount
              + " tasks, "
              + cycle.getMonitoredCredits()
              + " créditos e US$ "
              + cycle.getKnownCostUsd()
              + " monitorados.");
    }
    cycle.setBudgetAlertAt(observedAt);
    cycle.setUpdatedAt(observedAt);
    cycleRepository.save(cycle);
  }

  /** Usa a maior fotografia reconciliada para não duplicar nem ocultar o mesmo consumo. */
  private BigDecimal greatest(BigDecimal taskCost, BigDecimal ledgerCost) {
    BigDecimal tasks = taskCost == null ? BigDecimal.ZERO : taskCost;
    BigDecimal ledger = ledgerCost == null ? BigDecimal.ZERO : ledgerCost;
    return tasks.max(ledger);
  }

  /** Evita texto nulo no alerta persistido. */
  private String safeDetail(String detail) {
    return detail == null ? "" : detail;
  }
}
