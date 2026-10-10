package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private static final Logger log = LoggerFactory.getLogger(VideoNarrationRecovery.class);

  /** Impede instanciação do validador sem estado. */
  private VideoNarrationRecovery() {}

  /** Recupera somente a última finalização temporal falha da mesma fonte e tenant. */
  public static Map<String, Object> prepare(
      SalesVideoJob source,
      SalesVideoJob previous,
      RequestSalesVideoPostProductionRequest request,
      ObjectMapper mapper,
      AssetRepository assets) {
    if (previous == null
        || previous.getStatus() != SalesVideoStatus.VIDEO_FAILED
        || !"APOLLO_NARRATION_DURATION_EXCEEDED".equals(previous.getFailureCode())) return Map.of();
    require(
        Objects.equals(source.getTenantId(), previous.getTenantId())
            && previous.getRetryOfJob() != null
            && Objects.equals(previous.getRetryOfJob().getId(), source.getId()),
        "A voz pertence a outra fonte.");
    JsonNode prior = read(mapper, previous.getMetadataJson(), previous.getId());
    require(
        Objects.equals(prior.path("captionText").asText(), request.getCaptionText())
            && Objects.equals(prior.path("voiceOverScript").asText(), request.getVoiceOverScript()),
        "A recuperação preserva o texto e a voz anteriores; alteração de copy exige novo preflight.");
    require(
        request.getTargetDurationSeconds() != null,
        "Informe a duração final para recuperar a fala sem nova síntese.");
    List<String> texts =
        Arrays.stream(request.getCaptionText().split("\\s*\\|\\s*"))
            .map(String::trim)
            .filter(text -> !text.isBlank())
            .toList();
    JsonNode audit =
        previous.getEvents().stream()
            .filter(
                event ->
                    event.getDetailsJson() != null
                        && event.getDetailsJson().contains("persisted_audit_assets"))
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
      var asset =
          assets
              .findById(receipt.path("asset_id").asLong())
              .orElseThrow(() -> conflict("Áudio preservado não encontrado."));
      require(
          asset.getType() == AssetType.AUDIO
              && asset.getStatus() == AssetStatus.READY
              && asset.getUrl() != null
              && asset.getUrl().startsWith("https://"),
          "Áudio preservado indisponível.");
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
      return mapper.readTree(value == null ? "{}" : value);
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
