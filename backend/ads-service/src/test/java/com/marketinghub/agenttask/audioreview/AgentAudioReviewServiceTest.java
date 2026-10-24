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

/** Responsabilidade: homologar reserva, recuperação, isolamento e custo desconhecido de áudio. */
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

  /** O modelo auditivo não recebe response_format; o bloqueio antecede a reserva de consumo. */
  @ParameterizedTest
  @ValueSource(strings = {"json_schema", "json_object"})
  void rejectsUnsupportedStructuredOutputParametersBeforeSpending(String type) {
    var input = prepare(93005);
    ((com.fasterxml.jackson.databind.node.ObjectNode) input.request())
        .putObject("response_format")
        .put("type", type);
    assertThatThrownBy(() -> service.reserve("customer-agent", 9001, input))
        .hasMessageContaining("Campos inesperados");
    verify(reviews, never()).saveAndFlush(any());
    verify(ledger, never()).saveAndFlush(any());
  }

  /**
   * Reprocessa os corpos históricos sem rede, conservando sucesso, recusas, custo e imutabilidade.
   */
  @ParameterizedTest
  @ValueSource(ints = {3, 4, 5})
  void validatesHistoricalCapellaResponsesWithoutNewInference(int id) throws Exception {
    var input = prepare(93005);
    var media =
        json.createObjectNode()
            .put("assetId", 9003)
            .put("sha256", input.videoSha256())
            .put("durationSeconds", id == 3 ? 55 : 35);
    media.putObject("audioProductionEvidence").put("mix", "VOICE_ONLY").put("music", "none");
    when(videos.inputs(any())).thenReturn(json.createArrayNode().add(media));
    service.reserve("customer-agent", 9001, input);
    String raw =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "../../docs/diagnosticos/capella-openai-escuta-74/respostas/recibo-"
                    + id
                    + ".json"));
    var response = new AudioReviewResultRequest(raw);
    var result = service.complete("customer-agent", 9001, 9003, response);
    assertThat(result.status()).isEqualTo(id == 3 ? "COMPLETED" : id == 4 ? "REFUSED" : "INVALID");
    assertThat(result.estimatedUsd()).isNotNull();
    if (id == 5)
      assertThat(result.error()).contains("sem avaliação JSON", "sem repetição automática");
    assertThat(saved.get(9003L).getResponseJson()).isEqualTo(raw);
    assertThat(service.complete("customer-agent", 9001, 9003, response)).isEqualTo(result);
    assertThat(service.reserve("customer-agent", 9001, input).mayInvoke()).isFalse();
  }

  /** Reproduz dúvida do recibo 15 e outra identidade, preservando custo e bloqueio da aprovação. */
  @ParameterizedTest
  @ValueSource(longs = {10, 93010})
  void replaysPreservedNegativeAssessmentWithoutNewCharge(long productId) throws Exception {
    var input = prepare(productId);
    service.reserve("customer-agent", 9001, input);
    var media =
        json.createObjectNode()
            .put("assetId", 9003)
            .put("sha256", "a".repeat(64))
            .put("durationSeconds", 30);
    media.putObject("audioProductionEvidence").put("mix", "VOICE_ONLY").put("music", "none");
    when(videos.inputs(any())).thenReturn(json.createArrayNode().add(media));
    var assessment = AudioReviewContractTest.assessment(json).put("verdict", "ADJUST");
    ((com.fasterxml.jackson.databind.node.ObjectNode) assessment.path("music"))
        .put("status", "INCONCLUSIVE");
    var raw = json.createObjectNode().put("model", "gpt-audio-1.5");
    raw.putArray("choices")
        .addObject()
        .put("finish_reason", "stop")
        .putObject("message")
        .put("content", assessment.toString());
    var entry = saved.get(9003L);
    entry.setStatus("INVALID");
    entry.setResponseJson(raw.toString());
    entry.setEstimatedUsd(new BigDecimal("0.022693"));
    entry.setErrorMessage("O parecer descreve música, mas o mix auditado contém somente voz.");
    var request = new AudioReviewResultRequest(raw.toString());
    var result = service.complete("customer-agent", 9001, 9003, request);
    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(result.assessment().path("verdict").asText()).isEqualTo("ADJUST");
    assertThat(result.estimatedUsd()).isEqualByComparingTo("0.022693");
    assertThat(result.mayInvoke()).isFalse();
    assertThat(entry.getResponseJson()).isEqualTo(raw.toString());
    assertThat(service.complete("customer-agent", 9001, 9003, request)).isEqualTo(result);
    assertThat(service.reserve("customer-agent", 9001, input).mayInvoke()).isFalse();
    assertThatThrownBy(() -> service.validateCompletion(task, "{\"decision\":\"APPROVED\"}"))
        .hasMessageContaining("reprovada");
    verify(reviews, times(2)).saveAndFlush(any());
    verify(ledger, times(2)).saveAndFlush(any());
  }

  /** Replay não amplia a permissão a aprovação, JSON inválido ou identidade alterada. */
  @ParameterizedTest
  @ValueSource(strings = {"PASS", "MALFORMED", "OTHER_MEDIA"})
  void refusesUnsafeHistoricalReclassification(String variant) {
    var input = prepare(93010);
    service.reserve("customer-agent", 9001, input);
    var result =
        AudioReviewContractTest.assessment(json)
            .put("verdict", variant.equals("PASS") ? "PASS" : "ADJUST");
    var raw = json.createObjectNode().put("model", "gpt-audio-1.5");
    raw.putArray("choices")
        .addObject()
        .put("finish_reason", "stop")
        .putObject("message")
        .put("content", variant.equals("MALFORMED") ? "{incompleto" : result.toString());
    var entry = saved.get(9003L);
    entry.setStatus("INVALID");
    entry.setResponseJson(raw.toString());
    entry.setEstimatedUsd(new BigDecimal("0.022693"));
    if (variant.equals("OTHER_MEDIA")) entry.setVideoSha256("b".repeat(64));
    var receipt =
        service.complete(
            "customer-agent", 9001, 9003, new AudioReviewResultRequest(raw.toString()));
    assertThat(receipt.status()).isEqualTo("INVALID");
    assertThat(receipt.mayInvoke()).isFalse();
    assertThat(receipt.estimatedUsd()).isEqualByComparingTo("0.022693");
    verify(reviews, times(1)).saveAndFlush(any());
  }

  /** Duas identidades recebem causa de saldo correta, sem fatura, parecer ou nova chamada. */
  @ParameterizedTest
  @ValueSource(longs = {9005, 93005})
  void classifiesCreditRejectionBeforeModelValidation(long productId) {
    var input = prepare(productId);
    service.reserve("customer-agent", 9001, input);
    String raw =
        "{\"error\":{\"type\":\"insufficient_quota\",\"code\":\"credit_balance_exhausted\",\"message\":\"No credits remaining\"}}";
    var request = new AudioReviewResultRequest(raw);
    var result = service.complete("customer-agent", 9001, 9003, request);
    assertThat(result.status()).isEqualTo("ACCOUNT_BLOCKED");
    assertThat(result.estimatedUsd()).isZero();
    assertThat(result.assessment().isObject()).isTrue();
    assertThat(result.assessment().size()).isZero();
    assertThat(result.mayInvoke()).isFalse();
    assertThat(result.error())
        .contains(
            "credit_balance_exhausted", "API de áudio", "Codex", "não repetir automaticamente");
    assertThat(saved.get(9003L).getResponseJson()).isEqualTo(raw);
    assertThat(service.complete("customer-agent", 9001, 9003, request)).isEqualTo(result);
    assertThat(service.reserve("customer-agent", 9001, input).mayInvoke()).isFalse();
    var entries = org.mockito.ArgumentCaptor.forClass(StudioCostLedgerEntry.class);
    verify(ledger, times(2)).saveAndFlush(entries.capture());
    assertThat(entries.getValue().getCostEvidence())
        .isEqualTo("PROVIDER_ACCOUNT_REJECTION_NO_INFERENCE");
    assertThat(entries.getValue().getEstimatedCostUsd()).isZero();
    assertThat(entries.getValue().getProviderCostUsd()).isNull();
  }

  /** Reinterpreta o mesmo recibo antigo sem nova reserva ou perda da resposta preservada. */
  @Test
  void reconcilesHistoricalCostPendingCreditRejectionWithoutCallingProvider() {
    var input = prepare(93005);
    service.reserve("customer-agent", 9001, input);
    var entry = saved.get(9003L);
    String raw =
        "{\"error\":{\"type\":\"insufficient_quota\",\"code\":\"credit_balance_exhausted\"}}";
    entry.setResponseJson(raw);
    entry.setStatus("COST_PENDING");
    entry.setErrorMessage("Resposta de modelo incompatível.");
    var result = service.complete("customer-agent", 9001, 9003, new AudioReviewResultRequest(raw));
    assertThat(result.status()).isEqualTo("ACCOUNT_BLOCKED");
    assertThat(result.estimatedUsd()).isZero();
    assertThat(entry.getResponseJson()).isEqualTo(raw);
    assertThatThrownBy(
            () ->
                service.complete(
                    "customer-agent",
                    9001,
                    9003,
                    new AudioReviewResultRequest("{\"error\":{\"code\":\"other\"}}")))
        .hasMessageContaining("já foi preservada");
  }

  /**
   * Diferencia limites oficiais em duas identidades, preservando replay e recibo sem inferência.
   */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "organization_spend_limit_exceeded,9005", "organization_spend_limit_exceeded,93005",
    "project_spend_limit_exceeded,9005", "project_spend_limit_exceeded,93005",
    "organization_usage_limit_exceeded,9005", "organization_usage_limit_exceeded,93005"
  })
  void classifiesEnforcedLimitsAndReplaysHistoricalResponse(String code, long productId) {
    var input = prepare(productId);
    service.reserve("customer-agent", 9001, input);
    String raw = "{\"error\":{\"type\":\"insufficient_quota\",\"code\":\"" + code + "\"}}";
    var entry = saved.get(9003L);
    entry.setResponseJson(raw);
    entry.setStatus("COST_PENDING");
    entry.setErrorMessage("Resposta de modelo incompatível.");
    var request = new AudioReviewResultRequest(raw);
    var result = service.complete("customer-agent", 9001, 9003, request);
    assertThat(result.status()).isEqualTo("ACCOUNT_BLOCKED");
    assertThat(result.estimatedUsd()).isZero();
    assertThat(result.mayInvoke()).isFalse();
    assertThat(result.error()).contains(code, "não repetir automaticamente", "tetos próprios");
    String detail =
        switch (code) {
          case "organization_spend_limit_exceeded" -> "settings/organization/limits";
          case "project_spend_limit_exceeded" -> "configurações do projeto";
          default -> "limite de uso aprovado";
        };
    assertThat(result.error()).contains(detail);
    assertThat(result.assessment().size()).isZero();
    assertThat(entry.getResponseJson()).isEqualTo(raw);
    assertThat(service.complete("customer-agent", 9001, 9003, request)).isEqualTo(result);
    assertThat(service.reserve("customer-agent", 9001, input).mayInvoke()).isFalse();
  }

  /** Recusa ambígua, uso reportado ou custo já existente não podem virar custo zero. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "usage",
        "choices",
        "model",
        "other-code",
        "other-type",
        "duplicated",
        "charged-ledger"
      })
  void preservesUncertainRejectionCosts(String variant) {
    var input = prepare(9005);
    service.reserve("customer-agent", 9001, input);
    var raw = json.createObjectNode();
    raw.putObject("error")
        .put("type", "insufficient_quota")
        .put("code", "credit_balance_exhausted");
    switch (variant) {
      case "usage" -> raw.putObject("usage").put("prompt_tokens", 100);
      case "choices" -> raw.putArray("choices").addObject();
      case "model" -> raw.put("model", "gpt-audio-1.5");
      case "other-code" ->
          ((com.fasterxml.jackson.databind.node.ObjectNode) raw.path("error"))
              .put("code", "rate_limit_exceeded");
      case "other-type" ->
          ((com.fasterxml.jackson.databind.node.ObjectNode) raw.path("error"))
              .put("type", "unknown");
      case "charged-ledger" -> {
        var charged = new StudioCostLedgerEntry();
        charged.setProviderCostUsd(new BigDecimal("0.10"));
        when(ledger.findBySourceTypeAndSourceId("AGENT_AUDIO_REVIEW", "9100"))
            .thenReturn(Optional.of(charged));
      }
    }
    String body =
        variant.equals("duplicated")
            ? "{\"error\":{}," + raw.toString().substring(1)
            : raw.toString();
    var result = service.complete("customer-agent", 9001, 9003, new AudioReviewResultRequest(body));
    assertThat(result.status()).isEqualTo("COST_PENDING");
    assertThat(result.estimatedUsd()).isNull();
    assertThat(result.mayInvoke()).isFalse();
  }

  /** Falhas explícitas, truncamento e falta de texto bloqueiam mesmo com uso contabilizado. */
  @ParameterizedTest
  @ValueSource(strings = {"REFUSAL", "FILTER", "LENGTH", "NULL", "EMPTY", "MODEL", "RAW_DUPLICATE"})
  void rejectsProviderFailuresWithoutInventingAssessment(String variant) throws Exception {
    var input = prepare(93005);
    service.reserve("customer-agent", 9001, input);
    var raw = json.createObjectNode().put("model", "gpt-audio-1.5");
    var choice = raw.putArray("choices").addObject().put("finish_reason", "stop");
    var message =
        choice
            .putObject("message")
            .put("content", AudioReviewContractTest.assessment(json).toString());
    switch (variant) {
      case "REFUSAL" -> message.put("refusal", "Recusa explícita do provedor");
      case "FILTER" -> choice.put("finish_reason", "content_filter");
      case "LENGTH" -> choice.put("finish_reason", "length");
      case "NULL" -> message.putNull("content");
      case "EMPTY" -> message.put("content", " ");
      case "MODEL" -> raw.put("model", "gpt-audio-1.50");
      default -> {
        /* O caso de JSON bruto duplicado é construído após a serialização. */
      }
    }
    raw.set(
        "usage",
        json.readTree(
            "{\"prompt_tokens\":1100,\"prompt_tokens_details\":{\"audio_tokens\":1000},\"completion_tokens\":500}"));
    String body = raw.toString();
    if (variant.equals("RAW_DUPLICATE")) body = "{\"usage\":{}," + body.substring(1);
    var response = new AudioReviewResultRequest(body);
    var result = service.complete("customer-agent", 9001, 9003, response);
    assertThat(result.status())
        .isEqualTo(
            Set.of("RAW_DUPLICATE", "MODEL").contains(variant)
                ? "COST_PENDING"
                : Set.of("REFUSAL", "FILTER").contains(variant) ? "REFUSED" : "INVALID");
    if (Set.of("RAW_DUPLICATE", "MODEL").contains(variant))
      assertThat(result.estimatedUsd()).isNull();
    assertThat(saved.get(9003L).getResultJson()).isNull();
    assertThat(saved.get(9003L).getResponseJson()).isEqualTo(body);
    assertThat(service.complete("customer-agent", 9001, 9003, response)).isEqualTo(result);
    assertThat(service.reserve("customer-agent", 9001, input).mayInvoke()).isFalse();
  }

  /** JSON ambíguo não pode descartar uma recusa nem escolher silenciosamente o último campo. */
  @ParameterizedTest
  @ValueSource(strings = {"TRAILING", "DUPLICATE", "NUMERIC_SUMMARY", "MULTIPLE_CHOICES"})
  void rejectsAmbiguousOrNonConformingResponses(String variant) throws Exception {
    var input = prepare(93005);
    service.reserve("customer-agent", 9001, input);
    var assessment = AudioReviewContractTest.assessment(json);
    if (variant.equals("NUMERIC_SUMMARY"))
      ((com.fasterxml.jackson.databind.node.ObjectNode) assessment.path("voice"))
          .put("summary", new BigDecimal("12345678901234567890"));
    String content = assessment.toString();
    if (variant.equals("TRAILING")) content += " {\"listened\":false}";
    if (variant.equals("DUPLICATE"))
      content = content.replace("\"listened\":true", "\"listened\":false,\"listened\":true");
    var raw = json.createObjectNode().put("model", "gpt-audio-1.5");
    var choices = raw.putArray("choices");
    choices.addObject().put("finish_reason", "stop").putObject("message").put("content", content);
    if (variant.equals("MULTIPLE_CHOICES"))
      choices
          .addObject()
          .put("finish_reason", "content_filter")
          .putObject("message")
          .put("refusal", "Revisão recusada");
    raw.set(
        "usage",
        json.readTree(
            "{\"prompt_tokens\":1100,\"prompt_tokens_details\":{\"audio_tokens\":1000},\"completion_tokens\":500}"));
    var response = new AudioReviewResultRequest(raw.toString());
    var receipt = service.complete("customer-agent", 9001, 9003, response);
    assertThat(receipt.status()).isEqualTo("INVALID");
    assertThat(receipt.estimatedUsd()).isEqualByComparingTo("0.037250");
    assertThat(saved.get(9003L).getResponseJson()).isEqualTo(raw.toString());
    assertThat(saved.get(9003L).getResultJson()).isNull();
    assertThat(service.complete("customer-agent", 9001, 9003, response)).isEqualTo(receipt);
    assertThat(service.reserve("customer-agent", 9001, input).mayInvoke()).isFalse();
  }

  /** Uma nova tarefa reutiliza a escuta exata, sem duplicar reserva, resposta ou custo. */
  @ParameterizedTest
  @ValueSource(longs = {9005, 93005})
  void reusesExactCompletedReceiptAcrossTasks(long productId) throws Exception {
    var input = prepare(productId);
    var source = new AgentAudioReview();
    source.setId(8800L);
    source.setTaskId(8801L);
    source.setProductId(productId);
    source.setExperimentId(9002L);
    source.setAssetId(9003L);
    source.setVideoSha256(input.videoSha256());
    source.setAudioSha256(input.audioSha256());
    source.setStatus("COMPLETED");
    source.setEstimatedUsd(new BigDecimal("0.03"));
    source.setResultJson(AudioReviewContractTest.assessment(json).toString());
    String requestHash =
        AudioReviewContract.hash(
            input.request().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    when(reviews
            .findFirstByProductIdAndExperimentIdAndAssetIdAndVideoSha256AndAudioSha256AndRequestHashAndStatusAndEstimatedUsdIsNotNullOrderByIdDesc(
                productId,
                9002L,
                9003L,
                input.videoSha256(),
                input.audioSha256(),
                requestHash,
                "COMPLETED"))
        .thenReturn(Optional.of(source));
    var reused = service.reserve("customer-agent", 9001, input);
    assertThat(reused.id()).isEqualTo(8800L);
    assertThat(reused.mayInvoke()).isFalse();
    verify(reviews, never()).saveAndFlush(any());
    verify(ledger, never()).saveAndFlush(any());
    when(reviews.findById(8800L)).thenReturn(Optional.of(source));
    String result =
        "{\"decision\":\"APPROVED\",\"videoAudit\":[{\"assetId\":9003,\"sha256\":\""
            + input.videoSha256()
            + "\",\"audioReviewId\":8800,\"audioAssessment\":\"LISTENED\"}]}";
    assertThatCode(() -> service.validateCompletion(task, result)).doesNotThrowAnyException();
    source.setExperimentId(9999L);
    assertThatThrownBy(() -> service.validateCompletion(task, result))
        .hasMessageContaining("impede aprovação");
    source.setExperimentId(9002L);
    source.setProductId(productId + 1);
    assertThatThrownBy(() -> service.validateCompletion(task, result))
        .hasMessageContaining("impede aprovação");
    source.setProductId(productId);
    source.setEstimatedUsd(null);
    assertThatThrownBy(() -> service.validateCompletion(task, result))
        .hasMessageContaining("impede aprovação");
    source.setEstimatedUsd(new BigDecimal("0.03"));
    var changed = input.request().deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) changed).put("store", false);
    ((com.fasterxml.jackson.databind.node.ObjectNode) changed.at("/messages/0/content/0"))
        .put("text", "Contrato atualizado");
    assertThat(
            service
                .reserve(
                    "customer-agent",
                    9001,
                    new AudioReviewReserveRequest(
                        input.assetId(), input.videoSha256(), input.audioSha256(), changed))
                .mayInvoke())
        .isTrue();
  }

  /** Recusa real do provedor conserva custo e não vira aprovação ou autorização de repetição. */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void preservesRefusalAndDistinguishesUnknownCost(boolean knownCost) throws Exception {
    var input = prepare(9005);
    service.reserve("customer-agent", 9001, input);
    var raw = json.createObjectNode().put("model", "gpt-audio-1.5");
    raw.putArray("choices")
        .addObject()
        .put("finish_reason", "stop")
        .putObject("message")
        .put("content", "I'm sorry, but I can't comply with that request.");
    if (knownCost)
      raw.set(
          "usage",
          json.readTree(
              "{\"prompt_tokens\":2006,\"completion_tokens\":11,\"prompt_tokens_details\":{\"audio_tokens\":350,\"text_tokens\":1656}}"));
    var response = new AudioReviewResultRequest(raw.toString());
    var failed = service.complete("customer-agent", 9001, 9003, response);
    assertThat(failed.status()).isEqualTo(knownCost ? "REFUSED" : "COST_PENDING");
    assertThat(failed.error()).contains("recusou", "sem repetição automática");
    if (knownCost) assertThat(failed.estimatedUsd()).isEqualByComparingTo("0.015450");
    else assertThat(failed.estimatedUsd()).isNull();
    assertThat(saved.get(9003L).getResponseJson()).isEqualTo(raw.toString());
    assertThat(service.complete("customer-agent", 9001, 9003, response)).isEqualTo(failed);
    assertThat(service.reserve("customer-agent", 9001, input).mayInvoke()).isFalse();
    assertThatThrownBy(() -> service.validateCompletion(task, "{\"decision\":\"APPROVED\"}"))
        .hasMessageContaining("impede aprovação");
  }

  /** Preserva custo e resposta contraditória sem permitir aprovação por recibo antigo. */
  @Test
  void preservesContradictoryAudioWithoutApprovingIt() throws Exception {
    var input = prepare(9005);
    var media =
        json.createObjectNode()
            .put("assetId", 9003)
            .put("sha256", "a".repeat(64))
            .put("durationSeconds", 30);
    media.putObject("audioProductionEvidence").put("mix", "VOICE_ONLY").put("music", "none");
    when(videos.inputs(any())).thenReturn(json.createArrayNode().add(media));
    service.reserve("customer-agent", 9001, input);
    var raw = json.createObjectNode().put("model", "gpt-audio-1.5");
    raw.putArray("choices")
        .addObject()
        .put("finish_reason", "stop")
        .putObject("message")
        .put("content", AudioReviewContractTest.assessment(json).toString());
    raw.set(
        "usage",
        json.readTree(
            "{\"prompt_tokens\":1100,\"prompt_tokens_details\":{\"audio_tokens\":1000},\"completion_tokens\":500}"));
    var receipt =
        service.complete(
            "customer-agent", 9001, 9003, new AudioReviewResultRequest(raw.toString()));
    assertThat(receipt.status()).isEqualTo("INVALID");
    assertThat(receipt.estimatedUsd()).isEqualByComparingTo("0.037250");
    assertThat(saved.get(9003L).getResponseJson()).isEqualTo(raw.toString());
    assertThat(service.reserve("customer-agent", 9001, input).mayInvoke()).isFalse();
    // Recibo histórico aparentemente concluído precisa passar pelas mesmas regras no aceite final.
    saved.get(9003L).setStatus("COMPLETED");
    saved.get(9003L).setResultJson(AudioReviewContractTest.assessment(json).toString());
    assertThatThrownBy(() -> service.validateCompletion(task, "{\"decision\":\"APPROVED\"}"))
        .hasMessageContaining("somente voz");
  }

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
   * Estima PCM curto no teto existente e conserva bloqueio de saldo e replay para outros produtos.
   */
  @ParameterizedTest
  @ValueSource(longs = {10, 93010})
  void measuredAudioUsesExistingBudgetWithoutRepeatingCall(long productId) throws Exception {
    var original = prepare(productId);
    var buffer = new java.io.ByteArrayOutputStream();
    var format = new javax.sound.sampled.AudioFormat(24000, 16, 1, true, false);
    try (var stream =
        new javax.sound.sampled.AudioInputStream(
            new java.io.ByteArrayInputStream(new byte[33 * 48000]), format, 33 * 24000)) {
      javax.sound.sampled.AudioSystem.write(
          stream, javax.sound.sampled.AudioFileFormat.Type.WAVE, buffer);
    }
    byte[] wav = buffer.toByteArray();
    ((com.fasterxml.jackson.databind.node.ObjectNode)
            original.request().at("/messages/0/content/1/input_audio"))
        .put("data", Base64.getEncoder().encodeToString(wav));
    var input =
        new AudioReviewReserveRequest(
            original.assetId(),
            original.videoSha256(),
            AudioReviewContract.hash(wav),
            original.request());
    var used = new StudioCostLedgerEntry();
    used.setAssetType("VIDEO");
    used.setStatus("VIDEO_READY");
    used.setEstimatedCostUsd(new BigDecimal("9.80"));
    when(ledger.findByExperimentIdOrderByCreatedAtAsc(9002L)).thenReturn(List.of(used));
    assertThatThrownBy(() -> service.reserve("customer-agent", 9001, input))
        .hasMessageContaining("Saldo audiovisual insuficiente");
    verify(reviews, never()).saveAndFlush(any());
    used.setEstimatedCostUsd(new BigDecimal("9.70"));
    var receipt = service.reserve("customer-agent", 9001, input);
    assertThat(receipt.reservedUsd()).isEqualByComparingTo("0.25");
    assertThat(receipt.mayInvoke()).isTrue();
    assertThat(service.reserve("customer-agent", 9001, input).mayInvoke()).isFalse();
    verify(reviews, times(1)).saveAndFlush(any());
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
