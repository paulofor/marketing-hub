package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleConstructionPrerequisite;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar o mesmo predecessor em comandos diretos e na liberação da fila. */
class ProcessRunConstructionPrerequisiteTest {
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final BusinessProcessActivityExecutionService activities =
      mock(BusinessProcessActivityExecutionService.class);
  private final LearningCycleConstructionPrerequisite prerequisite =
      mock(LearningCycleConstructionPrerequisite.class);
  private final ProcessRunContext context =
      new ProcessRunContext(
          activities,
          processes,
          chains,
          cycles,
          mock(com.marketinghub.repository.jpa.product.ProductRepository.class),
          new ObjectMapper(),
          mock(com.marketinghub.repository.jpa.experiment.ExperimentRepository.class),
          mock(com.marketinghub.repository.jpa.agenttask.AgentTaskRepository.class));
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final ProcessRun waiting = new ProcessRun();
  private final ProcessRun candidate = new ProcessRun();
  private final LearningCycleConstructionPrerequisite.Pending pending =
      new LearningCycleConstructionPrerequisite.Pending(
          117L, 3, "Processo 3 pendente", "/construction?cycleId=21");

  /** Prepara o retorno direto à comunicação e o predecessor da mesma referência. */
  @BeforeEach
  void setup() {
    cycle.setId(21L);
    cycle.setProductId(4L);
    cycle.setChainDefinitionId(27L);
    cycle.setExperimentId(114L);
    cycle.setStatus("OPEN");
    for (var run : List.of(waiting, candidate)) {
      run.setProductId(4L);
      run.setChainDefinitionId(27L);
      run.setLearningCycleId(21L);
      run.setSourceReference("experiment:114");
    }
    waiting.setProcessDefinitionId(113L);
    waiting.setStatus("WAITING_INPUT");
    candidate.setProcessDefinitionId(117L);
    candidate.setStatus("QUEUED");
    when(cycles.findById(21L)).thenReturn(Optional.of(cycle));
    when(cycles.findByExperimentId(114L)).thenReturn(Optional.of(cycle));
    definition(113L, "pde-communication-sales-journey");
    definition(117L, "pde-construction-approval");
    when(prerequisite.pending(cycle)).thenReturn(pending);
    ReflectionTestUtils.setField(context, "constructionPrerequisites", prerequisite);
  }

  /** Define um processo publicado do próprio contexto, sem dados de outro produto. */
  private void definition(Long id, String code) {
    var process = new BusinessProcessDefinition();
    process.setId(id);
    process.setProcessCode(code);
    process.setStatus("PUBLISHED");
    process.setDiagramJson("{\"activities\":[],\"flows\":[]}");
    when(processes.findById(id)).thenReturn(Optional.of(process));
  }

  /** Abertura direta deve recusar a comunicação mesmo com o planejamento concluído. */
  @Test
  void rejectsDirectStartUntilConstructionApproved() {
    var chain = new BusinessProcessChainDefinition();
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(processes.findById(113L).orElseThrow());
    chain.setItems(List.of(item));
    when(chains.findById(27L)).thenReturn(Optional.of(chain));
    var history = mock(ProductProcessActivityExecutionHistoryResponse.class);
    when(history.currentExecutionReference()).thenReturn("experiment:114");
    when(activities.productProcessExecutions(113L, 4L, 21L, 27L, false, "experiment:114"))
        .thenReturn(history);
    assertThatThrownBy(
            () -> context.read(4L, 113L, new ProcessRunCommand(27L, 21L, "experiment:114"), true))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Processo 3 pendente");
  }

  /** O predecessor exato recebe a vez sem aprovar ou reiniciar comunicação. */
  @Test
  void letsConstructionPassWaitingCommunication() {
    assertThat(context.permitsCommercialContinuation(waiting, candidate)).isTrue();
    assertThat(waiting.getStatus()).isEqualTo("WAITING_INPUT");
    assertThat(context.constructionPrerequisite(waiting)).isEqualTo(pending);
    when(prerequisite.pending(cycle)).thenReturn(null);
    assertThat(context.constructionPrerequisite(waiting)).isNull();
    assertThat(context.permitsCommercialContinuation(waiting, candidate)).isFalse();
    verify(cycles, never()).save(any());
  }

  /** Outra identidade, estado protegido ou falha não ganha autorização para ultrapassar a fila. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "product", "chain", "cycle", "source", "process", "paused", "running", "failure", "closed"
      })
  void isolatesPrerequisiteContinuation(String change) {
    switch (change) {
      case "product" -> candidate.setProductId(7L);
      case "chain" -> candidate.setChainDefinitionId(28L);
      case "cycle" -> candidate.setLearningCycleId(22L);
      case "source" -> candidate.setSourceReference("experiment:111");
      case "process" -> candidate.setProcessDefinitionId(113L);
      case "paused" -> waiting.setStatus("PAUSED");
      case "running" -> waiting.setStatus("WAITING_ACTIVITY");
      case "failure" -> waiting.setFailureCount(1);
      case "closed" -> cycle.setStatus("CLOSED");
    }
    assertThat(context.permitsCommercialContinuation(waiting, candidate)).isFalse();
  }

  /** Omitir o ciclo ou misturar a identidade não permite contornar a verificação do início. */
  @ParameterizedTest
  @ValueSource(strings = {"product", "chain", "cycle", "missingCycle"})
  void rejectsWrongContextEvenWithExperimentReference(String change) {
    switch (change) {
      case "product" -> waiting.setProductId(7L);
      case "chain" -> waiting.setChainDefinitionId(28L);
      case "cycle" -> waiting.setLearningCycleId(22L);
      case "missingCycle" -> waiting.setLearningCycleId(null);
    }
    assertThatThrownBy(() -> context.constructionPrerequisite(waiting))
        .isInstanceOf(ResponseStatusException.class);
    verifyNoInteractions(prerequisite);
  }
}
