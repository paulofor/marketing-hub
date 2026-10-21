package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.LearningCycleDecisionApproval;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleResponse;
import com.marketinghub.experiment.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Responsabilidade: validar a recuperação real do ciclo estático sem inferência ou liberação
 * comercial.
 */
class LearningCycleWithoutAudiovisualTest {
  final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  final LearningSalesCycleEventRepository events = mock(LearningSalesCycleEventRepository.class);
  final ProductRepository products = mock(ProductRepository.class);
  final ExperimentRepository experiments = mock(ExperimentRepository.class);
  final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  final BusinessProcessDefinitionRepository definitions =
      mock(BusinessProcessDefinitionRepository.class);
  final LearningCycleBpmLedger ledger = mock(LearningCycleBpmLedger.class);
  final LearningCycleEvidence evidence = mock(LearningCycleEvidence.class);
  final LearningCycleVideoEvidence videos = mock(LearningCycleVideoEvidence.class);
  final LearningCycleOptionalAudiovisual optional = mock(LearningCycleOptionalAudiovisual.class);
  final LearningCycleWorkResolver work = mock(LearningCycleWorkResolver.class);
  final ObjectMapper mapper = new ObjectMapper();
  final LearningCycleService service =
      new LearningCycleService(
          cycles,
          events,
          products,
          experiments,
          chains,
          definitions,
          null,
          new LearningCycleJson(mapper),
          ledger,
          evidence,
          videos,
          null,
          null);
  final LearningSalesCycle cycle = new LearningSalesCycle();
  final List<LearningSalesCycleEvent> history = new ArrayList<>();

  /** Instala portas locais e a prova da decisão vigente, sem substituir os comandos reais. */
  @BeforeEach
  void setup() {
    cycle.setId(13L);
    cycle.setProductId(4L);
    cycle.setExperimentId(106L);
    cycle.setChainDefinitionId(26L);
    cycle.setProcessDefinitionId(125L);
    cycle.setProductVersion("candidate-v14");
    cycle.setStatus("OPEN");
    cycle.setStage("VIDEO_BRIEF");
    cycle.setRevision(4);
    cycle.setVersionChangedAt(Instant.now().minusSeconds(3600));
    cycle.setBriefJson("{}");
    cycle.setInheritedLearningJson("{}");
    var product = new Product();
    product.setId(4L);
    product.setAutomaticExecutionEnabled(true);
    when(products.findLockedById(4L)).thenReturn(Optional.of(product));
    when(products.findById(4L)).thenReturn(Optional.of(product));
    var experiment =
        Experiment.builder().id(106L).product(product).status(ExperimentStatus.PLANNED).build();
    when(experiments.findById(106L)).thenReturn(Optional.of(experiment));
    when(cycles.findLocked(4L, 13L)).thenReturn(Optional.of(cycle));
    var definition = new BusinessProcessDefinition();
    definition.setId(125L);
    definition.setVersionNumber(7);
    definition.setDiagramJson("{\"nodes\":[]}");
    when(definitions.findById(125L)).thenReturn(Optional.of(definition));
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(List.of());
    when(chains.findById(26L)).thenReturn(Optional.of(chain));
    when(work.resolvePreparation(cycle))
        .thenAnswer(
            invocation ->
                new LearningCycleWorkResolver.Resolution(
                    null, !"AUTHORIZATION".equals(cycle.getStage())));
    when(work.resolveStaticPreparation(cycle))
        .thenReturn(new LearningCycleWorkResolver.Resolution(null, true));
    var proof =
        mapper
            .createObjectNode()
            .put("contractVersion", "LEARNING_CYCLE_OPTIONAL_AUDIOVISUAL_V1")
            .put("omissionInstanceId", 719);
    when(optional.proof(cycle)).thenReturn(Optional.of(proof));
    when(events.findByCycleIdOrderByRevisionAsc(13L)).thenReturn(history);
    when(events.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              LearningSalesCycleEvent event = invocation.getArgument(0);
              history.add(event);
              return event;
            });
    ReflectionTestUtils.setField(service, "optionalAudiovisual", optional);
    ReflectionTestUtils.setField(service, "workResolver", work);
    ReflectionTestUtils.setField(service, "videoBudget", mock(LearningCycleVideoBudget.class));
    ReflectionTestUtils.setField(
        service, "decisionApproval", mock(LearningCycleDecisionApproval.class));
  }

  /** Duas passagens reais preservam a dispensa e o gate, sem aprovar orçamento ou publicação. */
  @Test
  void recoversAndValidatesStaticCycleIdempotently() {
    assertThat(service.continueWithoutAudiovisual(4L, 13L)).isTrue();
    assertThat(cycle.getStage()).isEqualTo("VALIDATION");
    assertThat(cycle.getRevision()).isEqualTo(5);
    assertThat(history).hasSize(1);
    assertThat(history.getFirst().getAction()).isEqualTo("RESOLVE_OPTIONAL_AUDIOVISUAL");
    verify(ledger)
        .finishAutomaticMeasurement(
            eq(cycle), anyString(), eq("NOT_APPLICABLE"), anyString(), any());
    assertThat(service.continueWithoutAudiovisual(4L, 13L)).isFalse();
    when(evidence.approvals(cycle))
        .thenReturn(List.of(new LearningCycleResponse.ApprovalOption(716L, "Gate vigente")));
    assertThat(service.continueWithoutAudiovisual(4L, 13L)).isTrue();
    assertThat(cycle.getStage()).isEqualTo("AUTHORIZATION");
    assertThat(history).hasSize(2);
    var validation = ArgumentCaptor.forClass(com.fasterxml.jackson.databind.JsonNode.class);
    verify(evidence).validation(eq(cycle), validation.capture());
    assertThat(validation.getValue().path("approvalInstanceId").asLong()).isEqualTo(716);
    assertThat(service.continueWithoutAudiovisual(4L, 13L)).isFalse();
    assertThat(history).hasSize(2);
    verifyNoInteractions(videos);
    verify(experiments, never()).save(any());
  }

  /** Parecer ausente e preparação incompleta continuam bloqueando o avanço. */
  @Test
  void missingProofOrIncompletePreparationNeverAdvances() {
    when(optional.proof(cycle)).thenReturn(Optional.empty());
    assertThat(service.continueWithoutAudiovisual(4L, 13L)).isFalse();
    assertThat(history).isEmpty();
    assertThat(cycle.getStage()).isEqualTo("VIDEO_BRIEF");
    when(optional.proof(cycle)).thenReturn(Optional.of(mapper.createObjectNode()));
    when(work.resolveStaticPreparation(cycle))
        .thenReturn(new LearningCycleWorkResolver.Resolution(null, false));
    assertThat(service.continueWithoutAudiovisual(4L, 13L)).isFalse();
    verifyNoInteractions(ledger);
  }

  /** STOP e liberação anterior impedem a recuperação mesmo quando o formato seria compatível. */
  @Test
  void stoppedProductOrReleasedExperimentNeverAdvances() {
    var product = products.findById(4L).orElseThrow();
    product.setAutomaticExecutionEnabled(false);
    assertThat(service.continueWithoutAudiovisual(4L, 13L)).isFalse();
    product.setAutomaticExecutionEnabled(true);
    experiments.findById(106L).orElseThrow().setStatus(ExperimentStatus.RUNNING);
    assertThat(service.continueWithoutAudiovisual(4L, 13L)).isFalse();
    assertThat(cycle.getStage()).isEqualTo("VIDEO_BRIEF");
    assertThat(history).isEmpty();
    verifyNoInteractions(ledger);
  }
}
