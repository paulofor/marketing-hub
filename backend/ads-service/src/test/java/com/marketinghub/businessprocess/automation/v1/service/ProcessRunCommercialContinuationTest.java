package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir fila circular sem liberar processos de outra ocorrência ou etapa. */
class ProcessRunCommercialContinuationTest {
  private final BusinessProcessDefinitionRepository definitions =
      mock(BusinessProcessDefinitionRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ProcessRunContext context =
      new ProcessRunContext(null, definitions, null, cycles, null, new ObjectMapper());
  private final ProcessRun waiting = new ProcessRun();
  private final ProcessRun candidate = new ProcessRun();
  private final LearningSalesCycle cycle = new LearningSalesCycle();

  /** Reproduz a espera do ciclo por uma versão específica de preparação da mesma cadeia. */
  @BeforeEach
  void setup() {
    for (var run : new ProcessRun[] {waiting, candidate}) {
      run.setProductId(4L);
      run.setChainDefinitionId(14L);
      run.setLearningCycleId(2L);
      run.setSourceReference("experiment:92");
    }
    waiting.setProcessDefinitionId(75L);
    waiting.setCurrentActivityId("learningCycle");
    waiting.setStatus("WAITING_ACTIVITY");
    candidate.setProcessDefinitionId(56L);
    cycle.setProductId(4L);
    cycle.setChainDefinitionId(14L);
    cycle.setExperimentId(92L);
    cycle.setStatus("OPEN");
    cycle.setStage("PUBLICATION");
    when(cycles.findById(2L)).thenReturn(Optional.of(cycle));
    definition(75L, "pde-sales-delivery-learning");
    definition(56L, "pde-commercial-homologation-activation");
  }

  /** Permite somente o processo preparador aguardado, sem aprovar nenhum de seus gates. */
  @ParameterizedTest
  @ValueSource(strings = {"AUTHORIZATION", "PUBLICATION"})
  void allowsRequiredPreparation(String stage) {
    cycle.setStage(stage);
    assertThat(context.permitsCommercialContinuation(waiting, candidate)).isTrue();
    verify(cycles, never()).save(any());
  }

  /** Mantém isolamento entre produto, cadeia, ciclo, experimento e versões não preparadoras. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "product",
        "chain",
        "cycle",
        "experiment",
        "process",
        "activity",
        "stage",
        "closed",
        "failure"
      })
  void refusesUnrelatedWork(String changed) {
    switch (changed) {
      case "product" -> candidate.setProductId(5L);
      case "chain" -> candidate.setChainDefinitionId(15L);
      case "cycle" -> candidate.setLearningCycleId(3L);
      case "experiment" -> candidate.setSourceReference("experiment:91");
      case "process" -> definition(56L, "pde-communication-sales-journey");
      case "activity" -> waiting.setCurrentActivityId("optimization");
      case "stage" -> cycle.setStage("MEASUREMENT");
      case "closed" -> cycle.setStatus("CLOSED");
      case "failure" -> waiting.setFailureCount(1);
      default -> throw new AssertionError(changed);
    }
    assertThat(context.permitsCommercialContinuation(waiting, candidate)).isFalse();
  }

  /** Uma pausa pendente, falha ou tarefa automática não autoriza ultrapassar a fila. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "PAUSING",
        "PAUSED",
        "ERROR",
        "CLOSED",
        "COMPLETED",
        "RUNNING",
        "BLOCKED",
        "WAITING_SUBPROCESS"
      })
  void preservesProtectedStates(String status) {
    waiting.setStatus(status);
    assertThat(context.permitsCommercialContinuation(waiting, candidate)).isFalse();
  }

  /** Registra as definições sintéticas com as responsabilidades canônicas de cada processo. */
  private void definition(long id, String code) {
    var definition = new BusinessProcessDefinition();
    definition.setId(id);
    definition.setProcessCode(code);
    when(definitions.findById(id)).thenReturn(Optional.of(definition));
  }
}
