package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.dto.RequestSalesVideoPostProductionRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Deriva um contrato de recuperação da fala recebida, sem autorizar outra síntese ou alterar copy.
 */
public final class VideoNarrationRecovery {
  public static final String CONTRACT = "PRESERVED_TTS_NARRATION_V1";
  public static final String SEQUENCE_CONTRACT = "PDE_VIDEO_POST_PRODUCTION_V2";
  public static final String DELIVERY_CONTRACT = "PDE_VIDEO_POST_PRODUCTION_V3";

  private static final Logger log = LoggerFactory.getLogger(VideoNarrationRecovery.class);

  /** Impede instanciação do validador sem estado. */
  private VideoNarrationRecovery() {}

  /**
   * Recupera a mesma voz ou registra mudança explícita de texto; o service valida rejeição e
   * financeiro. Recuperações sucessivas conservam os mesmos áudios e seu custo original pendente.
   */
  public static Map<String, Object> prepare(
      SalesVideoJob source,
      SalesVideoJob previous,
      RequestSalesVideoPostProductionRequest request,
      ObjectMapper mapper,
      AssetRepository assets) {
    if (previous == null) return Map.of();
    boolean completedFinalization =
        previous.getStatus() == SalesVideoStatus.VIDEO_READY
            && previous.getJobType()
                == com.marketinghub.salesvideo.SalesVideoJobType.POST_PRODUCTION;
    boolean durationFailure =
        previous.getStatus() == SalesVideoStatus.VIDEO_FAILED
            && java.util.Set.of(
                    "APOLLO_NARRATION_DURATION_EXCEEDED",
                    "PDE_PROOF_CAPTION_TIMING_INVALID",
                    "PDE_PRODUCT_PROOF_INVALID")
                .contains(previous.getFailureCode() == null ? "" : previous.getFailureCode());
    if (!completedFinalization && !durationFailure) return Map.of();
    require(
        Objects.equals(source.getTenantId(), previous.getTenantId())
            && previous.getRetryOfJob() != null
            && Objects.equals(previous.getRetryOfJob().getId(), source.getId()),
        "A voz pertence a outra fonte.");
    JsonNode prior = read(mapper, previous.getMetadataJson(), previous.getId());
    if (completedFinalization && prior.path("tts_interactions").isMissingNode()) {
      require(
          prior.path("preservedNarration").isMissingNode(),
          "A recuperação anterior está sem auditoria da fala; nova síntese não está autorizada.");
      return Map.of();
    }
    boolean sameCopy =
        Objects.equals(prior.path("captionText").asText(), request.getCaptionText())
            && Objects.equals(prior.path("voiceOverScript").asText(), request.getVoiceOverScript());
    if (!sameCopy && request.isNarrationRevisionConfirmed())
      return Map.of(
          "narrationRevision",
          Map.of(
              "contractVersion",
              "NARRATION_REVISION_V1",
              "previousJobId",
              previous.getId(),
              "newNarrationRequested",
              true,
              "providerCostReconciled",
              false));
    require(
        sameCopy,
        "A recuperação preserva o texto e a voz anteriores; confirme a revisão da locução dentro do limite audiovisual vigente.");
    require(
        request.getTargetDurationSeconds() != null,
        "Informe a duração final para recuperar a fala sem nova síntese.");
    List<String> texts =
        Arrays.stream(request.getCaptionText().split("\\s*\\|\\s*"))
            .map(String::trim)
            .filter(text -> !text.isBlank())
            .toList();
    JsonNode audit =
        completedFinalization
            ? prior
            : previous.getEvents().stream()
                .filter(
                    event ->
                        event.getDetailsJson() != null
                            && event.getDetailsJson().contains("tts_interactions"))
                .sorted(
                    Comparator.comparing(
                            (com.marketinghub.salesvideo.SalesVideoJobEvent event) -> event.getId(),
                            Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed())
                .map(event -> read(mapper, event.getDetailsJson(), previous.getId()))
                .findFirst()
                .orElseThrow(() -> conflict("Os áudios preservados não possuem recibo íntegro."));
    JsonNode interactions = audit.path("tts_interactions");
    require(
        interactions.isArray() && interactions.size() == texts.size(),
        "A auditoria da fala está incompleta.");
    List<Map<String, Object>> segments = new ArrayList<>();
    double total = 0;
    for (int index = 0; index < texts.size(); index++) {
      JsonNode interaction = interactions.get(index);
      if ("REUSED".equals(interaction.path("status").asText())) {
        Map<String, Object> segment =
            restoreRepeatedSegment(
                prior.path("preservedNarration"),
                interaction,
                index,
                texts,
                source,
                previous,
                request,
                mapper,
                assets);
        total += ((Number) segment.get("durationSeconds")).doubleValue();
        segments.add(segment);
        continue;
      }
      JsonNode response = interaction.path("raw_response");
      String hash = response.path("sha256").asText();
      require(
          interaction.path("segment_index").asInt() == index + 1
              && interaction.path("status").asText().equals("COMPLETED")
              && interaction.path("raw_request").path("input").asText().equals(texts.get(index))
              && hash.matches("[a-f0-9]{64}"),
          "Texto, ordem ou hash da fala divergente.");
      JsonNode receipt =
          matchingReceipt(audit.path("persisted_audit_assets"), response, previous.getId());
      var asset = readyAudio(receipt.path("asset_id").asLong(), assets);
      double duration = interaction.path("output_duration_seconds").asDouble();
      require(Double.isFinite(duration) && duration > 0, "Duração da fala não foi medida.");
      total += duration;
      Map<String, Object> segment = new LinkedHashMap<>();
      segment.put("segmentIndex", index + 1);
      segment.put("assetId", asset.getId());
      segment.put("url", asset.getUrl());
      segment.put("sha256", hash);
      segment.put("text", texts.get(index));
      segment.put("durationSeconds", duration);
      segment.put("model", interaction.path("model").asText());
      segment.put("voice", interaction.path("raw_request").path("voice").asText());
      segment.put("sourcePricing", mapper.convertValue(interaction.path("pricing"), Map.class));
      segments.add(segment);
    }
    require(
        request.getTargetDurationSeconds() >= 6
            && request.getTargetDurationSeconds() <= 60
            && request.getTargetDurationSeconds() >= total + 0.05,
        "A duração solicitada ainda não comporta a fala medida; nenhuma nova síntese foi iniciada.");
    return Map.of(
        "preservedNarration",
        Map.of(
            "contractVersion",
            "PRESERVED_TTS_NARRATION_V1",
            "sourceJobId",
            previous.getId(),
            "tenantId",
            previous.getTenantId(),
            "captionText",
            request.getCaptionText(),
            "measuredDurationSeconds",
            total,
            "newTtsAuthorized",
            false,
            "segments",
            segments));
  }

  /** Confere o recibo REUSED contra o contrato recebido, sem exigir novo upload nem inferência. */
  private static Map<String, Object> restoreRepeatedSegment(
      JsonNode contract,
      JsonNode receipt,
      int index,
      List<String> texts,
      SalesVideoJob source,
      SalesVideoJob previous,
      RequestSalesVideoPostProductionRequest request,
      ObjectMapper mapper,
      AssetRepository assets) {
    require(
        CONTRACT.equals(contract.path("contractVersion").asText())
            && !contract.path("newTtsAuthorized").asBoolean(true)
            && contract.path("sourceJobId").asLong() > 0
            && Objects.equals(contract.path("tenantId").asText(), source.getTenantId())
            && contract.path("captionText").asText().equals(request.getCaptionText())
            && contract.path("segments").isArray()
            && contract.path("segments").size() == texts.size(),
        "Contrato da recuperação anterior divergente.");
    JsonNode segment = contract.path("segments").path(index);
    String hash = segment.path("sha256").asText();
    double duration = segment.path("durationSeconds").asDouble();
    require(
        segment.path("segmentIndex").asInt() == index + 1
            && segment.path("text").asText().equals(texts.get(index))
            && segment.path("assetId").asLong() > 0
            && hash.matches("[a-f0-9]{64}")
            && Double.isFinite(duration)
            && duration > 0
            && !segment.path("model").asText().isBlank()
            && !segment.path("voice").asText().isBlank()
            && segment.path("sourcePricing").isObject(),
        "Segmento da recuperação anterior divergente.");
    require(
        "TEXT_TO_SPEECH_REUSE".equals(receipt.path("interaction_type").asText())
            && receipt.path("job_id").asLong() == previous.getId()
            && receipt.path("source_job_id").asLong() == contract.path("sourceJobId").asLong()
            && receipt.path("source_asset_id").asLong() == segment.path("assetId").asLong()
            && receipt.path("source_sha256").asText().equals(hash)
            && receipt.path("segment_index").asInt() == index + 1
            && receipt.path("model").asText().equals(segment.path("model").asText())
            && receipt.path("output_duration_seconds").isNumber()
            && Math.abs(receipt.path("output_duration_seconds").asDouble() - duration) < .001
            && receipt.path("incremental_cost_usd").isNumber()
            && receipt.path("incremental_cost_usd").asDouble() == 0
            && receipt.path("source_pricing").equals(segment.path("sourcePricing")),
        "Recibo da fala reutilizada divergente.");
    var asset = readyAudio(segment.path("assetId").asLong(), assets);
    Map<String, Object> restored = mapper.convertValue(segment, Map.class);
    restored.put("url", asset.getUrl());
    return restored;
  }

  /** Revalida a disponibilidade do áudio original a cada recuperação, usando a URL persistida. */
  private static Asset readyAudio(long id, AssetRepository assets) {
    var asset = assets.findById(id).orElseThrow(() -> conflict("Áudio preservado não encontrado."));
    require(
        asset.getType() == AssetType.AUDIO
            && asset.getStatus() == AssetStatus.READY
            && asset.getUrl() != null
            && asset.getUrl().startsWith("https://"),
        "Áudio preservado indisponível.");
    return asset;
  }

  /** Filtra antes da paginação conforme suporte a recuperação, captura e montagem de amostras. */
  public static org.springframework.data.jpa.domain.Specification<SalesVideoJob> consumableBy(
      String contract) {
    return (root, query, cb) -> {
      if (DELIVERY_CONTRACT.equals(contract)) return cb.conjunction();
      var noDelivery =
          cb.or(
              cb.isNull(root.get("metadataJson")),
              cb.notLike(root.get("metadataJson"), "%\"deliveryExamples\"%"));
      if (SEQUENCE_CONTRACT.equals(contract)) return noDelivery;
      var noSequence =
          cb.or(
              cb.isNull(root.get("metadataJson")),
              cb.notLike(root.get("metadataJson"), "%\"proofShots\"%"));
      if (CONTRACT.equals(contract)) return cb.and(noDelivery, noSequence);
      return cb.and(
          noDelivery,
          noSequence,
          cb.or(
              cb.isNull(root.get("metadataJson")),
              cb.notLike(root.get("metadataJson"), "%\"preservedNarration\"%")));
    };
  }

  /**
   * Impede que executor antigo ignore amostras, capturas ou voz preservada durante a publicação.
   */
  public static void requireCompatible(
      SalesVideoJob job, String workerContract, ObjectMapper mapper) {
    JsonNode metadata = read(mapper, job.getMetadataJson(), job.getId());
    if (!metadata.at("/post_production/deliveryExamples").isMissingNode())
      require(
          DELIVERY_CONTRACT.equals(workerContract),
          "O executor precisa declarar suporte à montagem de amostras antes de reservar este vídeo.");
    if (!metadata.at("/post_production/proofShots").isMissingNode())
      require(
          SEQUENCE_CONTRACT.equals(workerContract) || DELIVERY_CONTRACT.equals(workerContract),
          "O executor precisa declarar o contrato de sequência antes de reservar as capturas narradas.");
    if (!metadata.path("preservedNarration").isMissingNode())
      require(
          CONTRACT.equals(workerContract)
              || SEQUENCE_CONTRACT.equals(workerContract)
              || DELIVERY_CONTRACT.equals(workerContract),
          "O executor precisa declarar o contrato de recuperação antes de reservar a fala preservada.");
  }

  /** Exige recibo de upload com o mesmo nome e hash do binário recebido. */
  private static JsonNode matchingReceipt(JsonNode receipts, JsonNode response, Long jobId) {
    for (JsonNode receipt : receipts) {
      if (receipt.path("role").asText().equals("AUDIO_AUDIT")
          && receipt.path("file_name").asText().equals(response.path("asset_file_name").asText())
          && receipt.path("sha256").asText().equals(response.path("sha256").asText())
          && receipt.path("asset_id").asLong() > 0) return receipt;
    }
    throw conflict("Recibo de áudio ausente para a tentativa " + jobId + ".");
  }

  /** Lê a auditoria e conserva o stack trace quando o contrato estiver corrompido. */
  private static JsonNode read(ObjectMapper mapper, String value, Long jobId) {
    try {
      return mapper.readTree(value == null || value.isBlank() ? "{}" : value);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao ler auditoria de recuperação da fala; jobId={}", jobId, ex);
      throw conflict("Auditoria da fala inválida.");
    }
  }

  /** Bloqueia recuperação incompleta antes de qualquer consumo adicional. */
  private static void require(boolean valid, String message) {
    if (!valid) throw conflict(message);
  }

  /** Expõe o motivo de conflito ao formulário operacional. */
  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
