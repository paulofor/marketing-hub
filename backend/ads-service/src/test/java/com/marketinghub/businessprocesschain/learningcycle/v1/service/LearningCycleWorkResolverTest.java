package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.*;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: manter a continuidade na cadeia sem repetir tarefas ou saltar bloqueios. */
class LearningCycleWorkResolverTest {
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final BusinessProcessActivityExecutionService executions =
      mock(BusinessProcessActivityExecutionService.class);
  private final LearningCycleWorkResolver resolver =
      new LearningCycleWorkResolver(chains, executions);
  private final LearningSalesCycle cycle = new LearningSalesCycle();

  /** Modela o retorno ao planejamento seguido da construção na mesma cadeia. */
  @BeforeEach
  void setup() {
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setChainDefinitionId(14L);
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    cycle.setReturnProcessId(67L);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(
        new ArrayList<>(
            List.of(
                item(67, 2, "pde-commercial-plan-offer"),
                item(70, 3, "pde-construction-approval"))));
    when(chains.findById(14L)).thenReturn(Optional.of(chain));
  }

  /** Cria uma posição de processo sem inferir o número pelo identificador do banco. */
  private BusinessProcessChainItem item(long id, int number, String code) {
    var process = new BusinessProcessDefinition();
    process.setId(id);
    process.setProcessCode(code);
    process.setName(code);
    var item = new BusinessProcessChainItem();
    item.setSequenceNumber(number);
    item.setProcessDefinition(process);
    return item;
  }

  /** Delega a posição da atividade à mesma projeção oficial utilizada pela tela. */
  private void state(long id, boolean complete, String status) {
    var response = mock(ProductProcessActivityExecutionHistoryResponse.class);
    when(response.objectiveAchieved()).thenReturn(complete);
    if (!complete) {
      var activity = mock(ProductProcessActivityExecutionGroupResponse.class);
      when(activity.activityId()).thenReturn("deliverables");
      when(activity.activityName()).thenReturn("Produzir componentes");
      when(activity.activityOwnerName()).thenReturn("Dédalo");
      when(activity.sequenceNumber()).thenReturn(2);
      when(activity.operationalState()).thenReturn(status);
      when(activity.stateReason()).thenReturn("Estado persistido");
      when(response.currentActivityId()).thenReturn("deliverables");
      when(response.activities()).thenReturn(List.of(activity));
    }
    when(executions.productProcessExecutions(id, 4L, 2L, 14L)).thenReturn(response);
  }

  /** Processo concluído conduz à atividade seguinte mantendo produto, ciclo, cadeia e retorno. */
  @Test
  void completedPlanningContinuesConstruction() {
    state(67, true, null);
    state(70, false, "NOT_STARTED");
    var result = resolver.resolve(cycle);
    assertThat(result.processNumber()).isEqualTo(3);
    assertThat(result.activityNumber()).isEqualTo(2);
    assertThat(result.url())
        .isEqualTo(
            "/products/4/value-chain-history/processes/70/activities?learningCycleId=2&chainId=14#activity-deliverables");
    assertThat(cycle.getReturnProcessId()).isEqualTo(67L);
  }

  /** Bloqueio real no planejamento impede sugerir trabalho em um processo posterior. */
  @Test
  void preservesBlockedActivityInsteadOfSkippingIt() {
    state(67, false, "BLOCKED");
    assertThat(resolver.resolve(cycle).state()).isEqualTo("BLOCKED");
    verify(executions, never()).productProcessExecutions(70L, 4L, 2L, 14L);
  }

  /** Um processo completo não aprova a etapa ou o ciclo sem o comando e as provas exigidas. */
  @Test
  void completedPlanningDoesNotApproveAdjustmentAutomatically() {
    cycle.setStage("PLANNING");
    state(67, true, null);
    assertThat(resolver.resolve(cycle)).isNull();
    assertThat(cycle.getStage()).isEqualTo("PLANNING");
    verify(executions, never()).productProcessExecutions(70L, 4L, 2L, 14L);
  }

  /** Decisão encerrada e atividade chamadora não geram recursão nem novos comandos. */
  @Test
  void closedAndDecisionStagesHaveNoDelegatedWork() {
    cycle.setStatus("ADJUSTED");
    assertThat(resolver.resolve(cycle)).isNull();
    cycle.setStatus("OPEN");
    cycle.setStage("DECISION");
    assertThat(resolver.resolve(cycle)).isNull();
    verifyNoInteractions(executions);
  }
}
