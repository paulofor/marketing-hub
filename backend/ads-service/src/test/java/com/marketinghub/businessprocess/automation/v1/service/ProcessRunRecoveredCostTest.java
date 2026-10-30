package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import com.marketinghub.repository.jpa.processautomation.ProcessRunEventRepository;
import com.marketinghub.repository.jpa.processautomation.ProcessRunRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Responsabilidade: comprovar atualização de custos tardios sem reativar controles parados. */
class ProcessRunRecoveredCostTest {
  private final ProcessRunRepository runs = mock(ProcessRunRepository.class);
  private final ProcessRunEventRepository events = mock(ProcessRunEventRepository.class);
  private final ProcessRunContext context = mock(ProcessRunContext.class);
  private final ProcessRunSubprocesses subprocesses = mock(ProcessRunSubprocesses.class);
  private final BusinessProcessActivityExecutionService activities =
      mock(BusinessProcessActivityExecutionService.class);
  private ProcessRunService service;
  private ProcessRun run;
  private final ProcessRunCommand command = new ProcessRunCommand(96014L, null, "experiment:96021");

  /**
   * Consulta provas da mesma identidade e preserva pausa, custos históricos e ausência de tarefas.
   */
  @ParameterizedTest
  @CsvSource({
    "PAUSED, 96001",
    "COMPLETED, 96001",
    "CLOSED, 96001",
    "PAUSED, 96201",
    "COMPLETED, 96201",
    "CLOSED, 96201",
    "BLOCKED, 96001"
  })
  void showsRecoveredCostWithoutMutatingControl(String state, long productId) {
    prepare(state, productId, "COMPLETE", "11.301148");
    var result = service.status(productId, 96011L, command);
    assertThat(result.knownCostUsd()).isEqualByComparingTo("11.301148");
    assertThat(result.costCoverage()).isEqualTo("COMPLETE");
    assertThat(result.status()).isEqualTo(state);
    assertThat(result.completedActivities()).isEqualTo(3);
    assertThat(run.getKnownCostUsd()).isEqualByComparingTo("0.75");
    assertThat(run.getCostCoverage()).isEqualTo("PARTIAL");
    assertThat(run.getStatus()).isEqualTo(state);
    verify(context).read(same(run), eq(false));
    verify(runs, never()).save(any());
    verifyNoInteractions(activities, subprocesses, events);
  }

  /** Cobertura ausente ou parcial não transforma desconhecimento em custo zero. */
  @ParameterizedTest
  @ValueSource(strings = {"NO_EXECUTIONS", "NOT_REPORTED", "PARTIAL"})
  void preservesUnknownAndPartialCosts(String coverage) {
    prepare("PAUSED", 96001L, coverage, "1.25");
    var result = service.status(96001L, 96011L, command);
    assertThat(result.costCoverage()).isEqualTo(coverage);
    if ("PARTIAL".equals(coverage)) assertThat(result.knownCostUsd()).isEqualByComparingTo("1.25");
    else assertThat(result.knownCostUsd()).isNull();
    verifyNoInteractions(activities, subprocesses, events);
  }

  /** Controles ativos continuam usando a projeção persistida pelo reconciliador em execução. */
  @Test
  void preservesActiveControlWithoutAdditionalHistoryRead() {
    prepare("WAITING_ACTIVITY", 96001L, "COMPLETE", "11.301148");
    var result = service.status(96001L, 96011L, command);
    assertThat(result.knownCostUsd()).isEqualByComparingTo("0.75");
    assertThat(result.costCoverage()).isEqualTo("PARTIAL");
    verify(context, never()).read(any(ProcessRun.class), anyBoolean());
    verifyNoInteractions(activities, subprocesses, events);
  }

  /** Mostra a origem correta no card bloqueado sem disparar tarefa por leitura administrativa. */
  @Test
  void showsCorrectedOriginWithoutResumingOldBlock() {
    prepare("BLOCKED", 96001L, "COMPLETE", "11.30");
    run.setCurrentActivityId("productArchitecture");
    run.setCurrentOwnerName("Dédalo");
    var snapshot = context.read(run, false);
    when(snapshot.objectiveAchieved()).thenReturn(false);
    when(snapshot.currentActivityId()).thenReturn("marketStrategy");
    when(snapshot.currentActivityName()).thenReturn("Corrigir estratégia");
    when(snapshot.currentActivityStateReason())
        .thenReturn("Atena precisa preservar a entrada existente.");
    var activity =
        mock(
            com.marketinghub
                .businessprocess
                .execution
                .service
                .productProcessExecutions
                .ProductProcessActivityExecutionGroupResponse
                .class);
    when(activity.activityId()).thenReturn("marketStrategy");
    when(activity.selectedVersionActivity()).thenReturn(true);
    when(activity.activityOwnerName()).thenReturn("Atena");
    when(snapshot.activities()).thenReturn(List.of(activity));
    var response = service.status(96001L, 96011L, command);
    assertThat(response.currentActivityId()).isEqualTo("marketStrategy");
    assertThat(response.currentOwnerName()).isEqualTo("Atena");
    assertThat(response.reason()).contains("Correção pendente na origem");
    assertThat(run.getStatus()).isEqualTo("BLOCKED");
    assertThat(run.getCurrentActivityId()).isEqualTo("productArchitecture");
    verifyNoInteractions(activities, subprocesses, events);
  }

  /** Monta somente a consulta administrativa, com provas atuais distintas do recibo histórico. */
  private void prepare(String state, long productId, String coverage, String amount) {
    run = new ProcessRun();
    run.setId(96031L);
    run.setProductId(productId);
    run.setProcessDefinitionId(96011L);
    run.setSourceReference(command.sourceReference());
    run.setStatus(state);
    run.setKnownCostUsd(new BigDecimal("0.75"));
    run.setCostCoverage("PARTIAL");
    run.setTotalActivities(3);
    run.setCompletedActivities(2);
    run.setRemainingActivities(1);
    run.setOmittedActivities(0);
    when(runs.findByScopeKey(anyString())).thenReturn(Optional.of(run));
    var snapshot = mock(ProductProcessActivityExecutionHistoryResponse.class);
    when(snapshot.selectedActivityCount()).thenReturn(3);
    when(snapshot.completedActivityCount()).thenReturn(3);
    when(snapshot.objectiveAchieved()).thenReturn(true);
    when(snapshot.knownEstimatedCostUsd()).thenReturn(new BigDecimal(amount));
    when(snapshot.costCoverage()).thenReturn(coverage);
    when(snapshot.activities()).thenReturn(List.of());
    when(context.read(same(run), eq(false))).thenReturn(snapshot);
    var navigation = mock(ProcessRunNavigation.class);
    when(navigation.parents(any())).thenReturn(List.of());
    when(navigation.children(any())).thenReturn(List.of());
    var manager = mock(PlatformTransactionManager.class);
    when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    service =
        new ProcessRunService(
            runs,
            events,
            mock(ProductRepository.class),
            context,
            navigation,
            subprocesses,
            mock(ProcessRunGuidance.class),
            activities,
            new ObjectMapper(),
            manager,
            mock(ProcessRunOwnerPortrait.class));
  }
}
