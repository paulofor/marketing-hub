package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTaskProcessExecutionEvidenceSnapshot;
import com.marketinghub.agenttask.AgentTaskProcessExecutionListSnapshot;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  private final List<AgentTaskProcessExecutionListSnapshot> history = new java.util.ArrayList<>();
  private final Map<Long, AgentTaskProcessExecutionEvidenceSnapshot> evidence =
      new LinkedHashMap<>();
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
    snapshot.putObject("financialPlan").put("id", 1).put("revision", 1).put("status", "READY");
    snapshot.put("checkoutUrl", "https://checkout.sandbox.local/original");
    when(context.scope("experiment:92"))
        .thenReturn(
            new OpalaCommercialContext.Scope(
                cycle, Experiment.builder().id(92L).unitPrice(new BigDecimal("67")).build()));
    when(context.snapshot("experiment:92")).thenAnswer(i -> snapshot.deepCopy());
    when(context.read(anyString())).thenAnswer(i -> json.readTree((String) i.getArgument(0)));
    when(readiness.inspect(cycle))
        .thenReturn(
            new LearningCycleCommercialPreparation(true, "Pronto", "/experiments/92", List.of()));
    int index = 0;
    for (String step :
        List.of(
            "entry",
            "creative",
            "checkout",
            "targeting",
            "economics",
            "humanExperienceReview",
            "commercialIntegrityReview")) {
      long taskId = 100L + index++;
      history.add(summary(taskId, step, "COMPLETED"));
      evidence.put(taskId, evidence(taskId));
    }
    when(tasks.findProcessExecutionListSnapshots("experiment:92", OpalaCommercialContext.CODE))
        .thenReturn(history);
    when(tasks.findProcessExecutionEvidenceSnapshots(anyCollection()))
        .thenAnswer(
            invocation ->
                ((Collection<Long>) invocation.getArgument(0))
                    .stream().map(evidence::get).filter(java.util.Objects::nonNull).toList());
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
    evidence.replaceAll((taskId, ignored) -> evidence(taskId));
    gate.execute(process, activity, product, "experiment:92");
    verify(instances, times(2)).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getOccurrenceNumber()).isEqualTo(2);
  }

  /** Ignora somente ordem e representação numérica sem perder a identidade dos ativos revisados. */
  @Test
  void acceptsSemanticallyEqualReviewAssetsWithDifferentJsonOrdering() throws Exception {
    snapshot.set(
        "creatives", json.readTree("[{\"id\":529,\"score\":67.0},{\"id\":530,\"score\":100.00}]"));
    evidence.replaceAll((taskId, ignored) -> evidence(taskId));
    snapshot.set(
        "creatives", json.readTree("[{\"score\":100,\"id\":530},{\"score\":67,\"id\":529}]"));

    assertThat(gate.readiness(process, activity, product, "experiment:92").ready()).isTrue();
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

  /** Uma revisão bloqueada encerra o gate sem carregar evidências históricas extensas. */
  @Test
  void stopsAtBlockedReviewBeforeLoadingHistoricalEvidence() {
    int humanReviewIndex =
        java.util.stream.IntStream.range(0, history.size())
            .filter(index -> "humanExperienceReview".equals(history.get(index).processActivityId()))
            .findFirst()
            .orElseThrow();
    var blocked = history.get(humanReviewIndex);
    history.set(
        humanReviewIndex, summary(blocked.taskId(), blocked.processActivityId(), "BLOCKED"));

    assertThat(gate.readiness(process, activity, product, "experiment:92").ready()).isFalse();

    verify(tasks, never()).findProcessExecutionEvidenceSnapshots(anyCollection());
  }

  /** Cria o resumo ordenado de uma tarefa sem incluir prompts, resultados ou evidências. */
  private AgentTaskProcessExecutionListSnapshot summary(Long taskId, String step, String status) {
    return new AgentTaskProcessExecutionListSnapshot(
        taskId,
        process.getId(),
        process.getProcessCode(),
        1,
        step,
        status,
        "experiment:92",
        "psique",
        "Psique",
        step,
        step,
        null,
        null,
        null,
        null,
        null,
        "NOT_REPORTED",
        Instant.now(),
        null,
        null,
        Instant.now(),
        null,
        "MODEL",
        null,
        null,
        null,
        null);
  }

  /** Cria a evidência mínima da última aprovação exigida pelo gate comercial. */
  private AgentTaskProcessExecutionEvidenceSnapshot evidence(Long taskId) {
    return new AgentTaskProcessExecutionEvidenceSnapshot(
        taskId,
        json.createObjectNode().set("opalaScope", snapshot.deepCopy()).toString(),
        "{\"economics\":{\"offerPriceBrl\":67,\"deadline\":\""
            + LocalDate.now().plusDays(1)
            + "\"}}");
  }
}
