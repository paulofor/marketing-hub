package com.marketinghub.agenttask.audioreview;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.agenttask.audioreview.v1.*;
import com.marketinghub.agenttask.audioreview.v1.service.*;
import com.marketinghub.agenttask.audioreview.v1.service.reserve.*;
import com.marketinghub.agenttask.audioreview.v1.service.result.*;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.videoBudget.*;
import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agentaudioreview.*;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.financialagent.*;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.product.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Homologa reserva, recuperação, isolamento e custo desconhecido usando persistência simulada. */
class AgentAudioReviewServiceTest {
  private final ObjectMapper json = new ObjectMapper();
  private final AgentAudioReviewRepository reviews = mock(AgentAudioReviewRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final AgentTaskTargetContextProvider targets = mock(AgentTaskTargetContextProvider.class);
  private final CreativeFinalVideoEvidence videos = mock(CreativeFinalVideoEvidence.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningCycleVideoBudget budgets = mock(LearningCycleVideoBudget.class);
  private final StudioCostLedgerEntryRepository ledger =
      mock(StudioCostLedgerEntryRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final AgentAudioReviewService service =
      new AgentAudioReviewService(
          reviews, tasks, targets, videos, cycles, budgets, ledger, products, json);
  private AgentTask task;
  private LearningSalesCycle cycle;
  private final Map<Long, AgentAudioReview> saved = new HashMap<>();

  /** Monta duas identidades sem usar nenhum produto ou orçamento produtivo. */
  private AudioReviewReserveRequest prepare(long productId) {
    task = new AgentTask();
    task.setId(9001L);
    task.setSourceReference("experiment:9002");
    task.setStatus("IN_PROGRESS");
    task.setProcessActivityId("customer");
    var agent = new Agent();
    agent.setAgentKey("customer-agent");
    agent.setAutomaticExecutionEnabled(true);
    task.setAssignedAgent(agent);
    var process = new BusinessProcessDefinition();
    process.setProcessCode("creative-production-approval");
    task.setProcessDefinition(process);
    when(tasks.findLockedById(9001L)).thenReturn(Optional.of(task));
    var target =
        new AgentTaskTargetResponse(
            "experiment:9002",
            9002L,
            productId,
            "fixture",
            "Fixture",
            "Fixture",
            "v1",
            null,
            null,
            null,
            null,
            null,
            json.createObjectNode());
    when(targets.resolve("experiment:9002", "creative-production-approval"))
        .thenReturn(Optional.of(target));
    when(videos.inputs(target))
        .thenReturn(
            json.createArrayNode()
                .add(
                    json.createObjectNode()
                        .put("assetId", 9003)
                        .put("sha256", "a".repeat(64))
                        .put("durationSeconds", 30)));
    cycle = new LearningSalesCycle();
    cycle.setId(9004L);
    cycle.setProductId(productId);
    cycle.setExperimentId(9002L);
    cycle.setProductVersion("v1");
    cycle.setStatus("OPEN");
    when(cycles.findByExperimentId(9002L)).thenReturn(Optional.of(cycle));
    when(cycles.findLockedById(9004L)).thenReturn(Optional.of(cycle));
    var authorization = mock(VideoBudgetResponse.Authorization.class);
    when(authorization.budgetLimitUsd()).thenReturn(new BigDecimal("10"));
    when(budgets.current(cycle)).thenReturn(authorization);
    var product = new Product();
    product.setAutomaticExecutionEnabled(true);
    when(products.findById(productId)).thenReturn(Optional.of(product));
    when(reviews.findByTaskIdAndAssetId(9001L, 9003L))
        .thenAnswer(i -> Optional.ofNullable(saved.get(9003L)));
    when(reviews.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              AgentAudioReview e = i.getArgument(0);
              e.setId(9100L);
              saved.put(e.getAssetId(), e);
              return e;
            });
    byte[] wav = new byte[64];
    System.arraycopy("RIFF".getBytes(), 0, wav, 0, 4);
    System.arraycopy("WAVE".getBytes(), 0, wav, 8, 4);
    var request =
        json.createObjectNode()
            .put("model", "gpt-audio-1.5")
            .put("service_tier", "default")
            .put("store", false)
            .put("max_completion_tokens", 2000);
    request.putArray("modalities").add("text");
    var content = request.putArray("messages").addObject().put("role", "user").putArray("content");
    content.addObject().put("type", "text").put("text", "Fixture segregada");
    content
        .addObject()
        .put("type", "input_audio")
        .putObject("input_audio")
        .put("data", Base64.getEncoder().encodeToString(wav))
        .put("format", "wav");
    return new AudioReviewReserveRequest(
        9003L, "a".repeat(64), AudioReviewContract.hash(wav), request);
  }

  /**
   * A primeira chamada pode consumir; repetir reserva ou callback não duplica custo nem inferência.
   */
  @ParameterizedTest
  @ValueSource(longs = {9005, 93005})
  void reservesCompletesAndReplays(long productId) throws Exception {
    var input = prepare(productId);
    var first = service.reserve("customer-agent", 9001, input);
    assertThat(first.mayInvoke()).isTrue();
    assertThat(service.reserve("customer-agent", 9001, input).mayInvoke()).isFalse();
    var raw = json.createObjectNode().put("model", "gpt-audio-1.5");
    var choice = raw.putArray("choices").addObject().put("finish_reason", "stop");
    choice.putObject("message").put("content", AudioReviewContractTest.assessment(json).toString());
    raw.set(
        "usage",
        json.readTree(
            "{\"prompt_tokens\":1100,\"prompt_tokens_details\":{\"audio_tokens\":1000},\"completion_tokens\":500}"));
    var done =
        service.complete(
            "customer-agent", 9001, 9003, new AudioReviewResultRequest(raw.toString()));
    assertThat(done.status()).isEqualTo("COMPLETED");
    assertThat(done.estimatedUsd()).isEqualByComparingTo("0.037250");
    assertThat(
            service.complete(
                "customer-agent", 9001, 9003, new AudioReviewResultRequest(raw.toString())))
        .isEqualTo(done);
    assertThat(service.reserve("customer-agent", 9001, input).mayInvoke()).isFalse();
    assertThat(saved.get(9003L).getProductId()).isEqualTo(productId);
    var approved = json.createObjectNode().put("decision", "APPROVED");
    var audit =
        approved
            .putArray("videoAudit")
            .addObject()
            .put("assetId", 9003)
            .put("sha256", "a".repeat(64))
            .put("audioReviewId", done.id())
            .put("audioAssessment", "LISTENED");
    assertThatCode(() -> service.validateCompletion(task, approved.toString()))
        .doesNotThrowAnyException();
    audit.put("audioReviewId", done.id() + 1);
    assertThatThrownBy(() -> service.validateCompletion(task, approved.toString()))
        .hasMessageContaining("não cita");
    audit.put("audioReviewId", done.id());
    var rejected = AudioReviewContractTest.assessment(json).put("verdict", "ADJUST");
    saved.get(9003L).setResultJson(rejected.toString());
    assertThatThrownBy(() -> service.validateCompletion(task, approved.toString()))
        .hasMessageContaining("reprovada");
    assertThatThrownBy(() -> service.reserve("other-agent", 9001, input))
        .hasMessageContaining("Psique");
    assertThatThrownBy(
            () ->
                service.reserve(
                    "customer-agent",
                    9001,
                    new AudioReviewReserveRequest(
                        9003L, "b".repeat(64), input.audioSha256(), input.request())))
        .hasMessageContaining("outra entrada");
  }

  /**
   * Falha incerta continua reservada; custos incompletos, STOP e falta de autorização bloqueiam.
   */
  @Test
  void preservesUncertainSpendAndStopsBeforeProvider() {
    var input = prepare(9005);
    when(budgets.current(cycle)).thenReturn(null);
    assertThatThrownBy(() -> service.reserve("customer-agent", 9001, input))
        .hasMessageContaining("teto");
    verify(reviews, never()).saveAndFlush(any());
    var authorization = mock(VideoBudgetResponse.Authorization.class);
    when(authorization.budgetLimitUsd()).thenReturn(new BigDecimal("10"));
    when(budgets.current(cycle)).thenReturn(authorization);
    var secondInput = input;
    var unknown = new StudioCostLedgerEntry();
    unknown.setAssetType("AUDIO");
    unknown.setStatus("READY");
    when(ledger.findByExperimentIdOrderByCreatedAtAsc(9002L)).thenReturn(List.of(unknown));
    var unchanged = secondInput;
    assertThatThrownBy(() -> service.reserve("customer-agent", 9001, unchanged))
        .hasMessageContaining("desconhecido");
    when(ledger.findByExperimentIdOrderByCreatedAtAsc(9002L)).thenReturn(List.of());
    when(authorization.budgetLimitUsd()).thenReturn(new BigDecimal("4"));
    assertThatThrownBy(() -> service.reserve("customer-agent", 9001, unchanged))
        .hasMessageContaining("insuficiente");
    task.getAssignedAgent().setAutomaticExecutionEnabled(false);
    assertThatThrownBy(() -> service.reserve("customer-agent", 9001, unchanged))
        .hasMessageContaining("STOP");
  }

  /** JSON inválido fica preservado e jamais converte ausência de custo em zero. */
  @Test
  void preservesInvalidRawResponse() {
    var input = prepare(9005);
    service.reserve("customer-agent", 9001, input);
    var result =
        service.complete("customer-agent", 9001, 9003, new AudioReviewResultRequest("{invalid"));
    assertThat(result.status()).isEqualTo("COST_PENDING");
    assertThat(result.estimatedUsd()).isNull();
    assertThat(saved.get(9003L).getResponseJson()).isEqualTo("{invalid");
    assertThat(service.reserve("customer-agent", 9001, input).mayInvoke()).isFalse();
  }
}
