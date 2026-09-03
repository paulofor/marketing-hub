package com.marketinghub.salesvideo.autonomy.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.VideoProductionCycle;
import com.marketinghub.salesvideo.service.ApolloBudgetMonitorService;
import com.marketinghub.salesvideo.service.providerpreflight.VideoProviderFinancialPreflightService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: provar o monitor e o bloqueio financeiro imediato de Apolo. */
class ApolloBudgetMonitorServiceTest {
  /** Persiste alerta e totais quando uma nova task permanece dentro do teto. */
  @Test
  void deveAlertarNovaTaskDentroDoOrcamento() {
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    StudioCostLedgerService ledger = mock(StudioCostLedgerService.class);
    SalesVideoJobRepository jobs = mock(SalesVideoJobRepository.class);
    VideoProductionCycle cycle = cycle("20.00");
    when(cycles.findById(6L)).thenReturn(Optional.of(cycle));
    when(ledger.cycleProviderConsumption(6L))
        .thenReturn(
            java.util.Map.of("taskCount", 2L, "credits", 600L, "costUsd", new BigDecimal("6.00")));
    when(ledger.cycleKnownLedgerCostUsd(6L)).thenReturn(new BigDecimal("6.00"));

    new ApolloBudgetMonitorService(
            cycles, ledger, jobs, mock(VideoProviderFinancialPreflightService.class))
        .reconcile(6L, 21105L, "task-2");

    assertThat(cycle.getBudgetMonitorStatus()).isEqualTo("WATCHING");
    assertThat(cycle.getBudgetAlertCode()).isEqualTo("NEW_PROVIDER_TASK");
    assertThat(cycle.getMonitoredTaskCount()).isEqualTo(2L);
    assertThat(cycle.getMonitoredCredits()).isEqualTo(600L);
    assertThat(cycle.getKnownCostUsd()).isEqualByComparingTo("6.00");
    verify(cycles).save(cycle);
  }

  /** Interrompe o ciclo no primeiro callback que ultrapassa o teto aprovado. */
  @Test
  void deveBloquearCicloAoUltrapassarOrcamento() {
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    StudioCostLedgerService ledger = mock(StudioCostLedgerService.class);
    SalesVideoJobRepository jobs = mock(SalesVideoJobRepository.class);
    VideoProductionCycle cycle = cycle("5.00");
    SalesVideoJob job = new SalesVideoJob();
    job.setId(21105L);
    when(cycles.findById(6L)).thenReturn(Optional.of(cycle));
    when(ledger.cycleProviderConsumption(6L))
        .thenReturn(
            java.util.Map.of("taskCount", 2L, "credits", 600L, "costUsd", new BigDecimal("6.00")));
    when(ledger.cycleKnownLedgerCostUsd(6L)).thenReturn(new BigDecimal("6.00"));
    when(jobs.findById(21105L)).thenReturn(Optional.of(job));

    new ApolloBudgetMonitorService(
            cycles, ledger, jobs, mock(VideoProviderFinancialPreflightService.class))
        .reconcile(6L, 21105L, "task-2");

    assertThat(cycle.getStatus()).isEqualTo("APOLLO_BLOCKED");
    assertThat(cycle.getBudgetMonitorStatus()).isEqualTo("BLOCKED");
    assertThat(cycle.getBudgetAlertCode()).isEqualTo("BUDGET_EXCEEDED");
    assertThat(job.getStatus())
        .isEqualTo(com.marketinghub.salesvideo.SalesVideoStatus.VIDEO_FAILED);
    assertThat(job.getFailureCode()).isEqualTo("APOLLO_BUDGET_EXCEEDED");
    verify(cycles).save(cycle);
  }

  /** Preserva custo conhecido no ledger legado quando a tabela nova de tasks ainda está vazia. */
  @Test
  void deveConciliarCustoLegadoSemDuplicarConsumo() {
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    StudioCostLedgerService ledger = mock(StudioCostLedgerService.class);
    SalesVideoJobRepository jobs = mock(SalesVideoJobRepository.class);
    VideoProductionCycle cycle = cycle("10.00");
    when(cycles.findById(6L)).thenReturn(Optional.of(cycle));
    when(ledger.cycleProviderConsumption(6L))
        .thenReturn(java.util.Map.of("taskCount", 0L, "credits", 0L, "costUsd", BigDecimal.ZERO));
    when(ledger.cycleKnownLedgerCostUsd(6L)).thenReturn(new BigDecimal("1.20"));

    new ApolloBudgetMonitorService(
            cycles, ledger, jobs, mock(VideoProviderFinancialPreflightService.class))
        .reconcile(6L, 21105L, "legacy-job");

    assertThat(cycle.getKnownCostUsd()).isEqualByComparingTo("1.20");
    assertThat(cycle.getBudgetMonitorStatus()).isEqualTo("WATCHING");
  }

  /** Bloqueia imediatamente a geração quando o provider recusa por saldo insuficiente. */
  @Test
  void deveBloquearNaPrimeiraRecusaFinanceira() {
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    StudioCostLedgerService ledger = mock(StudioCostLedgerService.class);
    SalesVideoJobRepository jobs = mock(SalesVideoJobRepository.class);
    VideoProductionCycle cycle = cycle("20.00");
    when(cycles.findById(6L)).thenReturn(Optional.of(cycle));

    new ApolloBudgetMonitorService(
            cycles, ledger, jobs, mock(VideoProviderFinancialPreflightService.class))
        .blockForInsufficientCredits(6L, 21105L, "not enough credits");

    assertThat(cycle.getStatus()).isEqualTo("APOLLO_BLOCKED");
    assertThat(cycle.getBudgetAlertCode()).isEqualTo("INSUFFICIENT_CREDITS");
    assertThat(cycle.getBudgetAlertDetail()).contains("job #21105");
    verify(cycles).save(cycle);
  }

  /** Cria um ciclo mínimo monitorável. */
  private VideoProductionCycle cycle(String budget) {
    VideoProductionCycle cycle = new VideoProductionCycle();
    cycle.setId(6L);
    cycle.setStatus("QUEUED_FOR_APOLLO");
    cycle.setBudgetLimitUsd(new BigDecimal(budget));
    cycle.setKnownCostUsd(BigDecimal.ZERO);
    return cycle;
  }
}
