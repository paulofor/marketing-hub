package com.marketinghub.agenttask.audioreview.v1.service;

import static com.marketinghub.agenttask.audioreview.v1.service.AudioReviewContract.*;

import com.fasterxml.jackson.databind.*;
import com.marketinghub.agenttask.*;
import com.marketinghub.agenttask.audioreview.v1.AgentAudioReview;
import com.marketinghub.agenttask.audioreview.v1.service.receipt.AudioReviewReceipt;
import com.marketinghub.agenttask.audioreview.v1.service.reserve.AudioReviewReserveRequest;
import com.marketinghub.agenttask.audioreview.v1.service.result.AudioReviewResultRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleVideoBudget;
import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.repository.jpa.agentaudioreview.AgentAudioReviewRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: governar reserva, auditoria e recuperação da ferramenta auditiva de Psique. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentAudioReviewService {
  private final AgentAudioReviewRepository reviews;
  private final AgentTaskRepository tasks;
  private final AgentTaskTargetContextProvider targets;
  private final CreativeFinalVideoEvidence videos;
  private final LearningSalesCycleRepository cycles;
  private final LearningCycleVideoBudget budgets;
  private final StudioCostLedgerEntryRepository ledger;
  private final ProductRepository products;
  private final ObjectMapper json;

  /**
   * Reutiliza a escuta idêntica ou estima reserva pela entrada sob o lock do ciclo, sem repetir
   * envio incerto.
   */
  @Transactional
  public AudioReviewReceipt reserve(String agent, long taskId, AudioReviewReserveRequest input) {
    var task = owned(agent, taskId);
    require(input.assetId() != null && input.request() != null, "Entrada auditiva incompleta.");
    request(input.request(), input.audioSha256());
    String requestText = input.request().toString();
    String requestHash = hash(requestText.getBytes(StandardCharsets.UTF_8));
    var previous = reviews.findByTaskIdAndAssetId(taskId, input.assetId());
    if (previous.isPresent()) {
      var old = previous.get();
      require(
          old.getRequestHash().equals(requestHash)
              && old.getVideoSha256().equals(input.videoSha256())
              && old.getAudioSha256().equals(input.audioSha256()),
          "A chamada auditiva já identifica outra entrada.");
      return receipt(old, false);
    }
    require("IN_PROGRESS".equals(task.getStatus()), "Tarefa auditiva não está reservada.");
    require(
        Boolean.TRUE.equals(task.getAssignedAgent().getAutomaticExecutionEnabled()),
        "Psique está em STOP.");
    var target = target(task);
    var media = media(task, input.assetId());
    require(
        media.path("sha256").asText().equals(input.videoSha256()),
        "Vídeo alterado antes da escuta.");
    var cycle =
        cycles
            .findByExperimentId(target.experimentId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Escuta exige ciclo e teto audiovisual próprios."));
    cycle = cycles.findLockedById(cycle.getId()).orElseThrow();
    require(
        "OPEN".equals(cycle.getStatus())
            && Objects.equals(cycle.getProductId(), target.productId())
            && Objects.equals(cycle.getProductVersion(), target.experienceVersion()),
        "Ciclo/versão incompatível para escuta.");
    var product = products.findById(target.productId()).orElseThrow();
    require(Boolean.TRUE.equals(product.getAutomaticExecutionEnabled()), "Produto está em STOP.");
    var authority = budgets.current(cycle);
    require(authority != null, "Não há teto audiovisual autorizado para esta escuta.");
    var reusable =
        reviews
            .findFirstByProductIdAndExperimentIdAndAssetIdAndVideoSha256AndAudioSha256AndRequestHashAndStatusAndEstimatedUsdIsNotNullOrderByIdDesc(
                target.productId(),
                target.experimentId(),
                input.assetId(),
                input.videoSha256(),
                input.audioSha256(),
                requestHash,
                "COMPLETED");
    if (reusable.isPresent()) {
      var source = reusable.get();
      groundedAssessment(read(source.getResultJson()), media);
      log.info(
          "Escuta reutilizada sem inferência nem novo lançamento de custo. taskId={} sourceTaskId={} assetId={} reviewId={}",
          taskId,
          source.getTaskId(),
          input.assetId(),
          source.getId());
      return receipt(source, false);
    }
    var entries = ledger.findByExperimentIdOrderByCreatedAtAsc(target.experimentId());
    BigDecimal used = BigDecimal.ZERO;
    for (var entry : entries) {
      if (!Set.of("VIDEO", "AUDIO", "AUDIO_REVIEW").contains(entry.getAssetType())) continue;
      require(
          !Set.of("RESERVED", "COST_PENDING").contains(entry.getStatus()),
          "Há chamada audiovisual pendente de resposta ou conciliação.");
      BigDecimal cost =
          entry.getProviderCostUsd() != null
              ? entry.getProviderCostUsd()
              : entry.getEstimatedCostUsd();
      require(cost != null, "Há custo audiovisual desconhecido. Concilie antes da escuta.");
      used = used.add(cost);
    }
    for (var old : tasks.findMeasurementSnapshotsBySourceReference(task.getSourceReference())) {
      if (old.id().equals(taskId)
          || !"creative-production-approval".equals(old.processCode())
          || !"customer".equals(old.processActivityId())) continue;
      var result = read(old.resultJson());
      if (!result.path("videoAudit").isArray() || result.path("videoAudit").isEmpty()) continue;
      require(
          old.estimatedCostUsd() != null,
          "Custo de revisão audiovisual anterior ainda desconhecido.");
      used = used.add(old.estimatedCostUsd());
    }
    BigDecimal reservation = AudioReviewReservation.estimate(input.request());
    require(
        used.add(reservation).compareTo(authority.budgetLimitUsd()) <= 0,
        "Saldo audiovisual insuficiente para a reserva estimada da escuta (USD "
            + reservation
            + "); nenhum consumo foi iniciado.");
    var entry = new AgentAudioReview();
    entry.setTaskId(taskId);
    entry.setAssetId(input.assetId());
    entry.setProductId(target.productId());
    entry.setExperimentId(target.experimentId());
    entry.setVideoSha256(input.videoSha256());
    entry.setAudioSha256(input.audioSha256());
    entry.setRequestHash(requestHash);
    entry.setRequestJson(requestText);
    entry.setStatus("RESERVED");
    entry.setReservedUsd(reservation);
    entry.setStartedAt(Instant.now());
    entry = reviews.saveAndFlush(entry);
    saveLedger(entry);
    log.info(
        "Escuta reservada. taskId={} assetId={} reviewId={} experimentId={}",
        taskId,
        input.assetId(),
        entry.getId(),
        target.experimentId());
    return receipt(entry, true);
  }

  /**
   * Preserva respostas, identifica limites financeiros e valida o parecer somente quando houver
   * saída.
   */
  @Transactional
  public AudioReviewReceipt complete(
      String agent, long taskId, long assetId, AudioReviewResultRequest input) {
    var task = owned(agent, taskId);
    var entry = reviews.findByTaskIdAndAssetId(taskId, assetId).orElseThrow();
    require(
        input.rawResponse() != null && input.rawResponse().length() <= 200000,
        "Resposta auditiva inválida ou excessiva.");
    if (entry.getResponseJson() != null) {
      require(
          entry.getResponseJson().equals(input.rawResponse()),
          "A resposta desta chamada já foi preservada.");
      if ("COST_PENDING".equals(entry.getStatus()) && entry.getEstimatedUsd() == null) {
        try {
          if (classifyAccountLimit(entry, object(json, input.rawResponse()))) {
            reviews.saveAndFlush(entry);
            saveLedger(entry);
          }
        } catch (Exception ex) {
          log.warn(
              "Reclassificação da resposta auditiva preservada recusada. taskId={} assetId={} reviewId={}",
              taskId,
              assetId,
              entry.getId(),
              ex);
        }
      }
      if ("INVALID".equals(entry.getStatus())
          && entry.getEstimatedUsd() != null
          && entry.getResultJson() == null) {
        recoverNegativeAssessment(task, assetId, entry);
      }
      return receipt(entry, false);
    }
    entry.setResponseJson(input.rawResponse());
    entry.setFinishedAt(Instant.now());
    entry.setStatus("COST_PENDING");
    try {
      JsonNode raw = object(json, input.rawResponse());
      if (classifyAccountLimit(entry, raw)) {
        reviews.saveAndFlush(entry);
        saveLedger(entry);
        return receipt(entry, false);
      }
      model(raw);
      entry.setEstimatedUsd(cost(raw));
      if (entry.getEstimatedUsd() != null) entry.setStatus("INVALID");
      String content = raw.at("/choices/0/message/content").asText().trim();
      if (!raw.at("/choices/0/message/refusal").asText("").isBlank()
          || "content_filter".equals(raw.at("/choices/0/finish_reason").asText())
          || "I'm sorry, but I can't comply with that request.".equals(content)) {
        if (entry.getEstimatedUsd() != null) entry.setStatus("REFUSED");
        throw new IllegalArgumentException(
            "O provedor recusou a escuta; não existe avaliação sonora válida. Preservar resposta e custo; sem repetição automática.");
      }
      require(
          "stop".equals(raw.at("/choices/0/finish_reason").asText()),
          "Resposta auditiva incompleta ou recusada.");
      content = content(raw);
      require(
          content.startsWith("{"),
          "Resposta textual sem avaliação JSON; o schema no prompt não é Structured Outputs. "
              + "Áudio recebido não comprova escuta; preservar resposta/custo e diagnosticar, sem repetição automática.");
      var result = object(json, content);
      groundedAssessment(result, media(task, assetId));
      entry.setResultJson(result.toString());
      if (entry.getEstimatedUsd() != null) entry.setStatus("COMPLETED");
      else
        entry.setErrorMessage(
            "Uso por modalidade ausente ou fora dos limites; conciliação pendente.");
    } catch (Exception ex) {
      String message =
          ex instanceof com.fasterxml.jackson.core.JsonProcessingException
              ? "Resposta fora do contrato JSON: conteúdo incompleto, duplicado ou posterior ao objeto. "
                  + "Preservar resposta/custo e diagnosticar, sem repetição automática."
              : ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
      entry.setErrorMessage(message.substring(0, Math.min(1000, message.length())));
      log.error(
          "Falha ao validar resposta auditiva preservada. taskId={} assetId={} reviewId={}",
          taskId,
          assetId,
          entry.getId(),
          ex);
    }
    reviews.saveAndFlush(entry);
    saveLedger(entry);
    return receipt(entry, false);
  }

  /** Revalida somente parecer negativo preservado, sem nova inferência, custo ou aprovação. */
  private void recoverNegativeAssessment(AgentTask task, long assetId, AgentAudioReview entry) {
    try {
      var raw = object(json, entry.getResponseJson());
      require(
          "stop".equals(raw.at("/choices/0/finish_reason").asText())
              && raw.at("/choices/0/message/refusal").asText("").isBlank(),
          "Recusa ou resposta incompleta não admite recuperação.");
      var result = object(json, content(raw));
      require(
          Set.of("ADJUST", "INCONCLUSIVE").contains(result.path("verdict").asText()),
          "Recuperação negativa não transforma avaliação em aprovação.");
      var currentMedia = media(task, assetId);
      require(
          entry.getVideoSha256().equals(currentMedia.path("sha256").asText()),
          "A mídia mudou após a escuta preservada.");
      groundedAssessment(result, currentMedia);
      entry.setResultJson(result.toString());
      entry.setStatus("COMPLETED");
      entry.setErrorMessage(null);
      reviews.saveAndFlush(entry);
      saveLedger(entry);
      log.info(
          "Parecer sonoro negativo recuperado sem inferência. taskId={} assetId={} reviewId={} verdict={}",
          task.getId(),
          assetId,
          entry.getId(),
          result.path("verdict").asText());
    } catch (Exception ex) {
      log.warn(
          "Parecer sonoro preservado continua inválido. taskId={} assetId={} reviewId={}",
          task.getId(),
          assetId,
          entry.getId(),
          ex);
    }
  }

  /** Expõe recibos existentes sem reservar trabalho nem repetir chamadas de modelo. */
  @Transactional
  public List<AudioReviewReceipt> list(String agent, long taskId) {
    owned(agent, taskId);
    return reviews.findByTaskIdOrderByIdAsc(taskId).stream().map(e -> receipt(e, false)).toList();
  }

  /** Revalida a escuta citada, inclusive reutilizada, sem aceitar outra mídia ou contexto. */
  public void validateCompletion(AgentTask task, String resultJson) {
    if (!supports(task)) return;
    JsonNode result = read(resultJson);
    if (!"APPROVED".equals(result.path("decision").asText())) return;
    var target = target(task);
    for (var media : videos.inputs(target)) {
      Long citedId = null;
      for (var item : result.path("videoAudit"))
        if (item.path("assetId").asLong() == media.path("assetId").asLong())
          citedId = item.path("audioReviewId").asLong();
      var own = reviews.findByTaskIdAndAssetId(task.getId(), media.path("assetId").asLong());
      var entry =
          (own.isPresent()
                  ? own
                  : citedId == null
                      ? Optional.<AgentAudioReview>empty()
                      : reviews.findById(citedId))
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Aprovação de vídeo exige escuta persistida e vinculada ao parecer."));
      require(
          "COMPLETED".equals(entry.getStatus())
              && Objects.equals(entry.getProductId(), target.productId())
              && Objects.equals(entry.getExperimentId(), target.experimentId())
              && entry.getAssetId() == media.path("assetId").asLong()
              && entry.getEstimatedUsd() != null
              && entry.getVideoSha256().equals(media.path("sha256").asText())
              && "PASS".equals(read(entry.getResultJson()).path("verdict").asText()),
          "Escuta pendente ou reprovada impede aprovação audiovisual.");
      groundedAssessment(read(entry.getResultJson()), media);
      boolean matched = false;
      for (var item : result.path("videoAudit"))
        if (item.path("assetId").asLong() == entry.getAssetId()) {
          matched =
              entry.getVideoSha256().equals(item.path("sha256").asText())
                  && item.path("audioReviewId").asLong() == entry.getId()
                  && "LISTENED".equals(item.path("audioAssessment").asText());
        }
      require(matched, "Parecer final não cita a escuta da mesma mídia.");
    }
  }

  /**
   * Reclassifica recusa financeira sem repetir a chamada nem sobrescrever custo ou avaliação já
   * conhecidos.
   */
  private boolean classifyAccountLimit(AgentAudioReview review, JsonNode response) {
    if (!rejectedForAccountLimit(response)) return false;
    require(
        review.getEstimatedUsd() == null && review.getResultJson() == null,
        "Não substitua consumo ou avaliação existentes por uma recusa financeira.");
    var existing =
        ledger.findBySourceTypeAndSourceId("AGENT_AUDIO_REVIEW", review.getId().toString());
    require(
        existing.isEmpty()
            || (existing.get().getProviderCostUsd() == null
                && Set.of("RESERVATION_NOT_BILLED_COST", "PROVIDER_ACCOUNT_REJECTION_NO_INFERENCE")
                    .contains(Objects.toString(existing.get().getCostEvidence(), ""))),
        "O ledger já possui consumo que precisa ser preservado.");
    String previous = review.getStatus();
    review.setEstimatedUsd(BigDecimal.ZERO);
    review.setStatus("ACCOUNT_BLOCKED");
    String code = response.at("/error/code").asText();
    String action =
        switch (code) {
          case "organization_spend_limit_exceeded" ->
              "A organização atingiu o limite mensal de gasto configurado. Confira o limite em https://platform.openai.com/settings/organization/limits; adicionar créditos não altera esse limite.";
          case "project_spend_limit_exceeded" ->
              "O projeto atingiu o limite mensal de gasto configurado. Confira Limits nas configurações do projeto da chave de Psique; adicionar créditos não altera esse limite.";
          case "organization_usage_limit_exceeded" ->
              "A organização atingiu o limite de uso aprovado pela OpenAI. Solicite ampliação desse limite à OpenAI; ele é diferente do limite de gasto configurado.";
          default ->
              "A API de áudio de Psique está sem créditos. Disponibilize créditos na organização da chave de Psique.";
        };
    review.setErrorMessage(
        code
            + ": "
            + action
            + " A solicitação foi recusada antes da avaliação; custo incremental estimado zero. "
            + "Texto e visão do Worker IA usam Codex, mas esta escuta usa a API de áudio. "
            + "Regularize somente o controle indicado antes de nova tentativa; não repetir automaticamente. "
            + "Os tetos próprios do produto permanecem inalterados.");
    log.warn(
        "Recusa financeira auditiva identificada sem nova inferência. taskId={} assetId={} reviewId={} previousStatus={} status=ACCOUNT_BLOCKED endpoint=https://api.openai.com/v1/chat/completions code={}",
        review.getTaskId(),
        review.getAssetId(),
        review.getId(),
        previous,
        code);
    return true;
  }

  /** Mantém consumo no ledger existente, sem misturá-lo a tokens textuais do parecer principal. */
  private void saveLedger(AgentAudioReview review) {
    var entry =
        ledger
            .findBySourceTypeAndSourceId("AGENT_AUDIO_REVIEW", review.getId().toString())
            .orElseGet(StudioCostLedgerEntry::new);
    entry.setSourceType("AGENT_AUDIO_REVIEW");
    entry.setSourceId(review.getId().toString());
    entry.setAssetType("AUDIO_REVIEW");
    entry.setProductId(review.getProductId());
    entry.setExperimentId(review.getExperimentId());
    entry.setProvider("OPENAI");
    entry.setModel(MODEL);
    entry.setCurrency("USD");
    entry.setStatus(review.getStatus());
    entry.setEstimatedCostUsd(
        review.getEstimatedUsd() != null ? review.getEstimatedUsd() : review.getReservedUsd());
    entry.setCostEvidence(
        "ACCOUNT_BLOCKED".equals(review.getStatus())
            ? "PROVIDER_ACCOUNT_REJECTION_NO_INFERENCE"
            : review.getEstimatedUsd() != null
                ? "MODALITY_TOKEN_RATE_ESTIMATE"
                : "RESERVATION_NOT_BILLED_COST");
    entry.setStartedAt(review.getStartedAt());
    entry.setFinishedAt(review.getFinishedAt());
    ledger.saveAndFlush(entry);
  }

  /** Confirma identidade e contrato da tarefa sem confiar em IDs enviados pelo executor. */
  private AgentTask owned(String agent, long taskId) {
    var task = tasks.findLockedById(taskId).orElseThrow();
    require(
        "customer-agent".equals(agent) && supports(task),
        "A escuta pertence somente à tarefa criativa de Psique.");
    return task;
  }

  /** Restringe a capacidade à atividade criativa independente. */
  private boolean supports(AgentTask task) {
    return task.getAssignedAgent() != null
        && "customer-agent".equals(task.getAssignedAgent().getAgentKey())
        && task.getProcessDefinition() != null
        && "creative-production-approval".equals(task.getProcessDefinition().getProcessCode())
        && "customer".equals(task.getProcessActivityId());
  }

  /** Resolve o contexto pela fonte canônica do backend. */
  private AgentTaskTargetResponse target(AgentTask task) {
    return targets
        .resolve(task.getSourceReference(), task.getProcessDefinition().getProcessCode())
        .orElseThrow();
  }

  /** Confirma mídia atual do mesmo produto, experimento e versão. */
  private JsonNode media(AgentTask task, long assetId) {
    for (var item : videos.inputs(target(task)))
      if (item.path("assetId").asLong() == assetId) return item;
    throw new IllegalArgumentException("Mídia não pertence ao contexto atual da tarefa.");
  }

  /** Retorna diagnóstico recuperável sem expor o áudio bruto em respostas administrativas. */
  private AudioReviewReceipt receipt(AgentAudioReview entry, boolean mayInvoke) {
    return new AudioReviewReceipt(
        entry.getId(),
        entry.getAssetId(),
        entry.getVideoSha256(),
        entry.getAudioSha256(),
        entry.getStatus(),
        mayInvoke,
        entry.getReservedUsd(),
        entry.getEstimatedUsd(),
        read(entry.getResultJson()),
        "COMPLETED".equals(entry.getStatus())
            ? null
            : entry.getErrorMessage() != null
                ? entry.getErrorMessage()
                : "Escuta "
                    + entry.getStatus()
                    + "; preservar resposta e conciliar sem repetir inferência.");
  }

  /** Lê JSON persistido e mantém diagnóstico de conteúdo inválido. */
  private JsonNode read(String value) {
    try {
      return value == null ? json.createObjectNode() : object(json, value);
    } catch (Exception ex) {
      log.error("JSON de auditoria sonora inválido", ex);
      throw new IllegalArgumentException("Auditoria sonora inválida", ex);
    }
  }
}
