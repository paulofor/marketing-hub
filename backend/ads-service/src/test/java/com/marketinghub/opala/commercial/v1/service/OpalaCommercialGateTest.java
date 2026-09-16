package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleCommercialReadiness;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleCommercialPreparation;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: provar a consolidação real dos pareceres, a recuperação e a idempotência. */
class OpalaCommercialGateTest {
  private final ObjectMapper json = new ObjectMapper();
  private final OpalaCommercialContext context = mock(OpalaCommercialContext.class);
  private final LearningCycleCommercialReadiness readiness =
      mock(LearningCycleCommercialReadiness.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final OpalaCommercialGate gate =
      new OpalaCommercialGate(context, readiness, tasks, instances);
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final Product product = Product.builder().id(4L).build();
  private final BusinessProcessDefinition process = new BusinessProcessDefinition();
  private final BusinessProcessActivityDefinition activity =
      new BusinessProcessActivityDefinition();
  private final List<AgentTask> history = new ArrayList<>();
  private ObjectNode snapshot;

  /** Monta sete resultados persistidos do mesmo experimento, versão e orçamento sintéticos. */
  @BeforeEach
  void setup() throws Exception {
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setProductVersion("fixture-v12");
    cycle.setWindowEnd(Instant.now().plusSeconds(3600));
    process.setId(99L);
    process.setProcessCode(OpalaCommercialContext.CODE);
    activity.setId(108L);
    activity.setActivityId("ready");
    snapshot =
        json.createObjectNode()
            .put("productVersion", "fixture-v12")
            .put("cycleId", 2)
            .put("experimentId", 92);
    when(context.scope("experiment:92"))
        .thenReturn(
            new OpalaCommercialContext.Scope(
                cycle, Experiment.builder().id(92L).unitPrice(new BigDecimal("67")).build()));
    when(context.snapshot("experiment:92")).thenAnswer(i -> snapshot.deepCopy());
    when(context.read(anyString())).thenAnswer(i -> json.readTree((String) i.getArgument(0)));
    when(readiness.inspect(cycle))
        .thenReturn(
            new LearningCycleCommercialPreparation(true, "Pronto", "/experiments/92", List.of()));
    for (String step :
        List.of(
            "entry",
            "creative",
            "checkout",
            "targeting",
            "economics",
            "humanExperienceReview",
            "commercialIntegrityReview")) {
      var task = new AgentTask();
      task.setProcessActivityId(step);
      task.setStatus("COMPLETED");
      task.setEvidenceJson(
          json.createObjectNode().set("opalaScope", snapshot.deepCopy()).toString());
      task.setResultJson(
          "{\"economics\":{\"offerPriceBrl\":67,\"deadline\":\""
              + LocalDate.now().plusDays(1)
              + "\"}}");
      history.add(task);
    }
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            99L, "experiment:92"))
        .thenReturn(history);
  }

  /** Comprova prontidão uma única vez e registra expressamente que ainda não houve vendas. */
  @Test
  void completesAndRetriesWithoutDuplicatingEvidence() {
    assertThat(gate.execute(process, activity, product, "experiment:92").objectiveAchieved())
        .isTrue();
    var saved = ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getObjectiveEvidenceJson()).contains("\"salesProven\":false");
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            108L, "experiment:92"))
        .thenReturn(Optional.of(saved.getValue()));
    gate.execute(process, activity, product, "experiment:92");
    verify(instances, times(1)).saveAndFlush(any());
  }

  /** Reabre homologação se o ativo mudar e grava nova ocorrência após as novas revisões. */
  @Test
  void changedAssetsRequireNewReviewsAndNewProofEvenForSameVersion() {
    gate.execute(process, activity, product, "experiment:92");
    var saved = ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(saved.capture());
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            108L, "experiment:92"))
        .thenReturn(Optional.of(saved.getValue()));
    snapshot.put("checkoutUrl", "https://checkout.sandbox.local/revised");
    assertThat(gate.readiness(process, activity, product, "experiment:92").ready()).isFalse();
    for (var task : history)
      task.setEvidenceJson(
          json.createObjectNode().set("opalaScope", snapshot.deepCopy()).toString());
    gate.execute(process, activity, product, "experiment:92");
    verify(instances, times(2)).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getOccurrenceNumber()).isEqualTo(2);
  }

  /** Uma tarefa ausente ou uma janela expirada não fabrica conclusão. */
  @Test
  void refusesMissingTaskAndExpiredAuthorization() {
    history.removeFirst();
    assertThat(gate.readiness(process, activity, product, "experiment:92").reason())
        .contains("entry");
    cycle.setWindowEnd(Instant.now().minusSeconds(1));
    assertThat(gate.readiness(process, activity, product, "experiment:92").reason())
        .contains("expirou");
    verifyNoInteractions(instances);
  }
}
