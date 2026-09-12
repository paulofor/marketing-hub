package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.*;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

/**
 * Responsabilidade: impedir que conclusão técnica, outro ciclo ou repetição liberem o processo pai.
 */
class ProcessRunSubprocessesTest {
  private final BusinessProcessActivityDefinitionRepository definitions =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final ProcessRunSubprocesses service =
      new ProcessRunSubprocesses(definitions, instances, new ObjectMapper());
  private final ProductProcessActivityExecutionGroupResponse activity =
      mock(ProductProcessActivityExecutionGroupResponse.class);
  private final ProductProcessActivityExecutionHistoryResponse proof =
      mock(ProductProcessActivityExecutionHistoryResponse.class);
  private final ProcessRun parent = run(1L, 63L);
  private final ProcessRun child = run(2L, 64L);
  private final BusinessProcessActivityDefinition definition =
      new BusinessProcessActivityDefinition();

  /** Prepara identidade e prova funcional sintéticas para uma chamada pai-filho. */
  @BeforeEach
  void setup() {
    child.setParentRunId(1L);
    child.setStatus("COMPLETED");
    definition.setId(638L);
    definition.setActivityId("creatives");
    definition.setSubprocessCode("creative-production-approval");
    when(definitions.findByProcessDefinitionIdAndActivityId(63L, "creatives"))
        .thenReturn(Optional.of(definition));
    when(activity.activityId()).thenReturn("creatives");
    when(proof.productId()).thenReturn(4L);
    when(proof.selectedProcessDefinitionId()).thenReturn(64L);
    when(proof.processCode()).thenReturn("creative-production-approval");
    when(proof.currentExecutionReference()).thenReturn("experiment:92");
    when(proof.objectiveAchieved()).thenReturn(true);
    when(proof.completedActivityCount()).thenReturn(1);
    when(proof.remainingActivityCount()).thenReturn(0);
    var complete = mock(ProductProcessActivityExecutionGroupResponse.class);
    when(complete.activityId()).thenReturn("review");
    when(complete.selectedVersionActivity()).thenReturn(true);
    when(complete.operationalState()).thenReturn("COMPLETED");
    when(complete.objectiveAchieved()).thenReturn(true);
    when(complete.tasks()).thenReturn(List.of());
    when(proof.activities()).thenReturn(List.of(complete));
  }

  /**
   * Conclui a chamada com prova rastreável e custo segregado, sem duplicar ocorrência em replay.
   */
  @Test
  void persistsProofAndAvoidsDuplicateCompletion() throws Exception {
    service.complete(parent, activity, child, proof);
    var saved = ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(saved.capture());
    var instance = saved.getValue();
    assertThat(instance.isObjectiveAchieved()).isTrue();
    assertThat(
            new ObjectMapper()
                .readTree(instance.getObjectiveEvidenceJson())
                .path("childRunId")
                .asLong())
        .isEqualTo(2);
    assertThat(instance.getKnownCostUsd()).isZero();
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            638L, "experiment:92"))
        .thenReturn(Optional.of(instance));
    service.complete(parent, activity, child, proof);
    verify(instances).saveAndFlush(any());
  }

  /** Recusa tarefa terminada sem objetivo e relações de outro ciclo antes de qualquer escrita. */
  @Test
  void refusesUnprovenOrForeignContext() {
    when(proof.objectiveAchieved()).thenReturn(false);
    assertThatThrownBy(() -> service.complete(parent, activity, child, proof))
        .hasMessageContaining("não comprovou");
    when(proof.objectiveAchieved()).thenReturn(true);
    child.setLearningCycleId(3L);
    assertThatThrownBy(() -> service.complete(parent, activity, child, proof))
        .hasMessageContaining("mesmo contexto");
    verifyNoInteractions(instances);
  }

  /** Cria uma execução de teste sem acessar dados produtivos. */
  private ProcessRun run(Long id, Long process) {
    var r = new ProcessRun();
    r.setId(id);
    r.setProductId(4L);
    r.setProcessDefinitionId(process);
    r.setChainDefinitionId(14L);
    r.setLearningCycleId(2L);
    r.setSourceReference("experiment:92");
    r.setCreatedAt(Instant.now());
    return r;
  }
}
