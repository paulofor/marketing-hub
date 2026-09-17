package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.opala.commercial.v1.service.OpalaCommercialRouting;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: garantir reutilização rastreável dos pareceres sem nova chamada paga. */
class PdeCommercialReviewReuseActivityExecutorTest {
  private final ObjectMapper json = new ObjectMapper();
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final OpalaCommercialRouting routing = mock(OpalaCommercialRouting.class);
  private final BusinessProcessActivityDefinitionRepository definitions =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final PdeCommercialReviewReuseActivityExecutor executor =
      new PdeCommercialReviewReuseActivityExecutor(
          json, predecessors, cycles, routing, definitions, tasks, instances);
  private final Product product = Product.builder().id(4L).build();
  private final BusinessProcessDefinition parent =
      process(56L, "pde-commercial-homologation-activation", 7);
  private final BusinessProcessDefinition child =
      process(77L, "opala-commercial-preparation-v1", 1);
  private final BusinessProcessActivityDefinition activity = reviewActivity();
  private final AgentTask sourceTask = new AgentTask();
  private final BusinessProcessActivityInstance readyInstance =
      new BusinessProcessActivityInstance();

  /** Prepara provas vigentes no mesmo ciclo e subprocesso. */
  @BeforeEach
  void setup() {
    var cycle = new LearningSalesCycle();
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    sourceTask.setId(428L);
    sourceTask.setProcessActivityId("humanExperienceReview");
    sourceTask.setStatus("COMPLETED");
    sourceTask.setEvidenceJson("{\"opalaScope\":{\"version\":\"v12\"}}");
    readyInstance.setId(292L);
    readyInstance.setStatus("COMPLETED");
    readyInstance.setObjectiveAchieved(true);
    readyInstance.setObjectiveEvidenceJson("{\"productVersion\":\"v12\"}");
    var readyDefinition = new BusinessProcessActivityDefinition();
    readyDefinition.setId(789L);
    when(predecessors.readiness(parent, activity, "experiment:92"))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Preparação concluída."));
    when(cycles.findByExperimentId(92L)).thenReturn(Optional.of(cycle));
    when(routing.isOpala(4L)).thenReturn(true);
    when(routing.target(cycle)).thenReturn(child);
    when(routing.completed(cycle)).thenReturn(true);
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            77L, "experiment:92"))
        .thenReturn(List.of(sourceTask));
    when(definitions.findByProcessDefinitionIdAndActivityId(77L, "ready"))
        .thenReturn(Optional.of(readyDefinition));
    when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            789L, "experiment:92"))
        .thenReturn(Optional.of(readyInstance));
  }

  /** Registra referência e impressão da prova original com custo incremental zero. */
  @Test
  void reusesCurrentReviewWithoutDuplicatingAgentCost() throws Exception {
    var result = executor.execute(parent, activity, product, "experiment:92");

    assertThat(result.objectiveAchieved()).isTrue();
    var saved = ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getKnownCostUsd()).isZero();
    assertThat(saved.getValue().getEvidenceQuality()).isEqualTo("REUSED_DIRECT");
    var evidence = json.readTree(saved.getValue().getObjectiveEvidenceJson());
    assertThat(evidence.path("sourceTaskId").asLong()).isEqualTo(428L);
    assertThat(evidence.path("sourceReadyInstanceId").asLong()).isEqualTo(292L);
    assertThat(evidence.path("costRecordedInSubprocess").asBoolean()).isTrue();
  }

  /** Bloqueia o reaproveitamento se o gate do subprocesso detectar mudança material. */
  @Test
  void blocksReuseWhenCommercialSnapshotChanged() {
    when(routing.completed(org.mockito.ArgumentMatchers.any())).thenReturn(false);

    var result = executor.readiness(parent, activity, product, "experiment:92");

    assertThat(result.ready()).isFalse();
    assertThat(result.reason()).contains("revalide a preparação Opala");
  }

  /** Cria a atividade revisora com o contrato explícito de reaproveitamento. */
  private BusinessProcessActivityDefinition reviewActivity() {
    var definition = new BusinessProcessActivityDefinition();
    definition.setId(562L);
    definition.setActivityId("humanExperienceReview");
    definition.setDefinitionJson(
        "{\"evidenceReuseVersion\":\"COMMERCIAL_REVIEW_REUSE_V1\",\"reuseSubprocessActivityId\":\"humanExperienceReview\"}");
    return definition;
  }

  /** Cria uma definição mínima com identidade e versão imutáveis. */
  private BusinessProcessDefinition process(Long id, String code, int version) {
    var definition = new BusinessProcessDefinition();
    definition.setId(id);
    definition.setProcessCode(code);
    definition.setVersionNumber(version);
    definition.setStatus("PUBLISHED");
    return definition;
  }
}
