package com.marketinghub.financialagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.financialagent.StudioProviderTaskConsumption;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import com.marketinghub.repository.jpa.financialagent.StudioProviderTaskConsumptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: provar a conciliação financeira por task mesmo quando o job não concluir. */
class StudioProviderTaskConsumptionTest {
  /** Replay do aceite preserva a cobrança liquidada e recusa reaproveitamento em outro job. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {21105L, 91105L})
  void devePreservarConciliacaoOriginalDuranteRecuperacao(Long jobId) {
    var ledger = mock(StudioCostLedgerEntryRepository.class);
    var tasks = mock(StudioProviderTaskConsumptionRepository.class);
    var task = new StudioProviderTaskConsumption();
    task.setSalesVideoJobId(jobId);
    task.setVideoProductionCycleId(6L);
    task.setModel("gen4.5");
    task.setSceneNumber(1);
    task.setPlannedSceneCount(4);
    task.setDurationSeconds(10);
    task.setEstimatedCredits(120);
    task.setEstimatedCostUsd(new BigDecimal("1.20"));
    task.setAcceptedAt(Instant.parse("2026-11-05T18:00:00Z"));
    task.setBilledCostUsd(new BigDecimal("1.20"));
    task.setSettlementStatus("CONTRACTUAL_CHARGE");
    when(tasks.findByProviderAndProviderTaskId("RUNWAY", "task-" + jobId))
        .thenReturn(Optional.of(task));
    var service = new StudioCostLedgerService(ledger, tasks);
    service.recordProviderTask(
        jobId,
        6L,
        "RUNWAY",
        "task-" + jobId,
        "gen4.5",
        1,
        4,
        10,
        120,
        new BigDecimal("1.200000"),
        Instant.now(),
        "VIDEO_PROCESSING");
    assertThat(task.getAcceptedAt()).isEqualTo(Instant.parse("2026-11-05T18:00:00Z"));
    assertThat(task.getBilledCostUsd()).isEqualByComparingTo("1.20");
    assertThat(task.getSettlementStatus()).isEqualTo("CONTRACTUAL_CHARGE");
    assertThatThrownBy(
            () ->
                service.recordProviderTask(
                    jobId + 1,
                    6L,
                    "RUNWAY",
                    "task-" + jobId,
                    "gen4.5",
                    1,
                    4,
                    10,
                    120,
                    new BigDecimal("1.20"),
                    Instant.now(),
                    "VIDEO_PROCESSING"))
        .hasMessageContaining("outro contexto");
    verify(tasks, never()).save(any());
    verifyNoInteractions(ledger);
  }

  /** Persiste a cena idempotente e atualiza o custo parcial conhecido do job falho. */
  @Test
  void deveConciliarTaskAceitaAntesDaFalhaDoJob() {
    StudioCostLedgerEntryRepository ledger = mock(StudioCostLedgerEntryRepository.class);
    StudioProviderTaskConsumptionRepository tasks =
        mock(StudioProviderTaskConsumptionRepository.class);
    StudioCostLedgerEntry jobEntry = new StudioCostLedgerEntry();
    when(tasks.findByProviderAndProviderTaskId("RUNWAY", "task-1")).thenReturn(Optional.empty());
    when(tasks.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(tasks.sumEstimatedCostUsdBySalesVideoJobId(21105L)).thenReturn(new BigDecimal("3.00"));
    when(ledger.findBySourceTypeAndSourceId("SALES_VIDEO_JOB", "21105"))
        .thenReturn(Optional.of(jobEntry));

    new StudioCostLedgerService(ledger, tasks)
        .recordProviderTask(
            21105L,
            6L,
            "RUNWAY",
            "task-1",
            "seedance2_5",
            1,
            3,
            10,
            300,
            new BigDecimal("3.00"),
            Instant.parse("2026-08-13T18:10:35Z"),
            "VIDEO_PROCESSING");

    ArgumentCaptor<StudioProviderTaskConsumption> task =
        ArgumentCaptor.forClass(StudioProviderTaskConsumption.class);
    verify(tasks).save(task.capture());
    assertThat(task.getValue().getProviderTaskId()).isEqualTo("task-1");
    assertThat(task.getValue().getEstimatedCredits()).isEqualTo(300);
    assertThat(jobEntry.getEstimatedCostUsd()).isEqualByComparingTo("3.00");
    assertThat(jobEntry.getCostEvidence()).isEqualTo("PROVIDER_TASK_RATE_CARD_ESTIMATE");

    when(ledger.findBySourceTypeAndSourceId("SALES_VIDEO_JOB", "21105"))
        .thenReturn(Optional.of(jobEntry));
    new StudioCostLedgerService(ledger, tasks)
        .recordVideo(
            21105L,
            6L,
            1L,
            2L,
            null,
            "VIDEO",
            "RUNWAY_SEEDANCE_2_5",
            "seedance2_5",
            "VIDEO_FAILED",
            null,
            false,
            Instant.parse("2026-08-13T18:10:00Z"),
            Instant.parse("2026-08-13T18:11:00Z"));

    assertThat(jobEntry.getStatus()).isEqualTo("VIDEO_FAILED");
    assertThat(jobEntry.getEstimatedCostUsd()).isEqualByComparingTo("3.00");
    assertThat(jobEntry.getCostEvidence()).isEqualTo("PROVIDER_TASK_RATE_CARD_ESTIMATE");
  }

  /** Substitui a estimativa pelo débito liquidado quando a task conclui. */
  @Test
  void deveLiquidarCustoDaTaskPeloDesfechoDoProvider() {
    StudioCostLedgerEntryRepository ledger = mock(StudioCostLedgerEntryRepository.class);
    StudioProviderTaskConsumptionRepository tasks =
        mock(StudioProviderTaskConsumptionRepository.class);
    StudioProviderTaskConsumption task = new StudioProviderTaskConsumption();
    StudioCostLedgerEntry jobEntry = new StudioCostLedgerEntry();
    when(tasks.findByProviderAndProviderTaskId("RUNWAY", "task-1")).thenReturn(Optional.of(task));
    when(tasks.sumBilledCostUsdBySalesVideoJobId(21105L)).thenReturn(new BigDecimal("3.00"));
    when(tasks.countBySalesVideoJobIdAndSettlementStatusIsNull(21105L)).thenReturn(0L);
    when(ledger.findBySourceTypeAndSourceId("SALES_VIDEO_JOB", "21105"))
        .thenReturn(Optional.of(jobEntry));

    new StudioCostLedgerService(ledger, tasks)
        .settleProviderTask(
            21105L,
            "RUNWAY",
            "task-1",
            300,
            new BigDecimal("3.00"),
            "CONTRACTUAL_CHARGE",
            "CONTRACTUAL_RATE_CARD",
            "PROVIDER_RATE_CARD_AND_TASK_SUCCESS",
            Instant.parse("2026-08-13T18:11:00Z"),
            "VIDEO_PROCESSING");

    assertThat(task.getBilledCredits()).isEqualTo(300);
    assertThat(task.getSettlementStatus()).isEqualTo("CONTRACTUAL_CHARGE");
    assertThat(task.getSettlementBasis()).isEqualTo("CONTRACTUAL_RATE_CARD");
    assertThat(jobEntry.getProviderCostUsd()).isEqualByComparingTo("3.00");
    assertThat(jobEntry.getEstimatedCostUsd()).isNull();
    assertThat(jobEntry.getCostEvidence()).isEqualTo("PROVIDER_TASKS_SETTLED_BY_CONTRACT");
  }
}
