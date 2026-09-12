package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.*;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir espera sem orientação e autorização financeira de outro contexto. */
class ProcessRunGuidanceTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final ProcessRunGuidance guidance =
      new ProcessRunGuidance(
          cycles,
          processes,
          new LearningCycleVideoBudget(events, new LearningCycleJson(new ObjectMapper())));
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final ProcessRun run = new ProcessRun();
  private final BusinessProcessDefinition parent = new BusinessProcessDefinition();
  private final Instant now = Instant.parse("2026-09-12T23:00:00Z");

  /** Reproduz os identificadores do incidente com fontes locais isoladas. */
  @BeforeEach
  void setup() {
    run.setProductId(4L);
    run.setProcessDefinitionId(75L);
    run.setChainDefinitionId(14L);
    run.setLearningCycleId(2L);
    run.setSourceReference("experiment:92");
    run.setCurrentActivityId("learningCycle");
    run.setStatus("WAITING_ACTIVITY");
    parent.setProcessCode("pde-sales-delivery-learning");
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setChainDefinitionId(14L);
    cycle.setProcessDefinitionId(76L);
    cycle.setProductVersion("musa-v12");
    cycle.setVersionChangedAt(now.minusSeconds(30));
    cycle.setStatus("OPEN");
    cycle.setStage("VIDEO_BRIEF");
    var definition = new BusinessProcessDefinition();
    definition.setVersionNumber(4);
    when(cycles.findById(2L)).thenReturn(Optional.of(cycle));
    when(processes.findById(75L)).thenReturn(Optional.of(parent));
    when(processes.findById(76L)).thenReturn(Optional.of(definition));
  }

  /** Expõe uma ação financeira explícita sem alterar ciclo, progresso ou criar consumo. */
  @Test
  void missingBudgetDirectsToExactFinanceContext() {
    var action = guidance.resolve(run);
    assertThat(action.code()).isEqualTo("AUTHORIZE_VIDEO_BUDGET");
    assertThat(action.actionUrl()).isEqualTo("/financial/videos?productId=4&chainId=14&cycleId=2");
    assertThat(action.reason()).contains("USD", "anúncio", "demonstração", "Aguardar");
    assertThat(action.afterAction()).contains("avaliação financeira", "não autoriza mídia");
    assertThat(action.evidenceReference()).isNull();
    assertThat(run.getStatus()).isEqualTo("WAITING_ACTIVITY");
    assertThat(cycle.getStage()).isEqualTo("VIDEO_BRIEF");
    verify(cycles, never()).save(any());
    verify(events, never()).saveAndFlush(any());
  }

  /** Usa o ledger financeiro real para mudar a orientação depois da autorização desta versão. */
  @Test
  void recordedBudgetDirectsToBriefWithoutClaimingCompletion() {
    authorize("musa-v12");
    var action = guidance.resolve(run);
    assertThat(action.code()).isEqualTo("COMPLETE_VIDEO_BRIEF");
    assertThat(action.actionUrl())
        .isEqualTo("/business-process-chains/learning-cycles?chainId=14&productId=4&cycleId=2");
    assertThat(action.evidenceReference())
        .isEqualTo("internal://learning-cycles/2/video-budget/local");
    assertThat(action.afterAction()).contains("não conclui o processo");
  }

  /** Mantém a pendência quando a autorização pertence à versão anterior. */
  @Test
  void previousVersionDoesNotAuthorizeCurrentVideos() {
    authorize("musa-v11");
    assertThat(guidance.resolve(run).code()).isEqualTo("AUTHORIZE_VIDEO_BUDGET");
  }

  /** Recusa contexto divergente antes de ler ou apresentar qualquer autorização. */
  @ParameterizedTest
  @ValueSource(strings = {"product", "chain", "experiment"})
  void refusesAnotherContext(String field) {
    switch (field) {
      case "product" -> cycle.setProductId(5L);
      case "chain" -> cycle.setChainDefinitionId(15L);
      case "experiment" -> cycle.setExperimentId(91L);
      default -> throw new AssertionError(field);
    }
    assertThatThrownBy(() -> guidance.resolve(run)).hasMessageContaining("outro contexto");
    verifyNoInteractions(events);
  }

  /** Preserva pausa, término, bloqueios técnicos e espera de subprocessos reais. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "PAUSED",
        "PAUSING",
        "COMPLETED",
        "CLOSED",
        "ERROR",
        "BLOCKED",
        "WAITING_SUBPROCESS",
        "WAITING_PARENT"
      })
  void doesNotReplaceOtherRunStates(String status) {
    run.setStatus(status);
    assertThat(guidance.resolve(run)).isNull();
    verifyNoInteractions(cycles, events);
  }

  /** Etapas de produção e medição mantêm seus contratos próprios de espera. */
  @ParameterizedTest
  @ValueSource(strings = {"CAMPAIGN_VIDEO", "PDE_ENTRY_VIDEO", "MEASUREMENT", "ADJUSTMENT"})
  void otherStagesDoNotRequestBudgetAgain(String stage) {
    cycle.setStage(stage);
    assertThat(guidance.resolve(run)).isNull();
    verifyNoInteractions(events);
  }

  /** Outro processo ou atividade de agente em execução não recebe decisão do ciclo. */
  @Test
  void doesNotReplaceAgentExecutionOrAnotherProcess() {
    run.setCurrentActivityId("produce");
    assertThat(guidance.resolve(run)).isNull();
    run.setCurrentActivityId("learningCycle");
    parent.setProcessCode("pde-communication-sales-journey");
    assertThat(guidance.resolve(run)).isNull();
    verifyNoInteractions(events);
  }

  /** O histórico encerrado não volta a solicitar uma autorização financeira. */
  @Test
  void closedCyclePreservesHistory() {
    cycle.setStatus("STOPPED");
    assertThat(guidance.resolve(run)).isNull();
    verifyNoInteractions(events);
  }

  /** A pausa reconhece que não há execução automática, mas não oferece ação ao usuário. */
  @Test
  void pausingManualCycleDoesNotWaitForUnstartedWork() {
    run.setStatus("PAUSING");
    assertThat(guidance.awaitingInput(run)).isTrue();
    assertThat(guidance.resolve(run)).isNull();
    verifyNoInteractions(events);
  }

  /** Cria somente uma prova financeira sintética do contrato canônico. */
  private void authorize(String version) {
    var event = new LearningSalesCycleEvent();
    event.setId(10L);
    event.setCreatedAt(now);
    event.setEvidenceReference("internal://learning-cycles/2/video-budget/local");
    event.setEvidenceJson(
        """
        {"productVersion":"%s", "experimentId":92, "budgetLimitUsd":20.50,
         "scope":"TWO_VIDEOS_PRODUCTION_AND_REVIEW", "currency":"USD"}
        """
            .formatted(version));
    when(events.findByCycleIdAndActionOrderByRevisionDesc(2L, "AUTHORIZE_VIDEO_BUDGET"))
        .thenReturn(List.of(event));
  }
}
