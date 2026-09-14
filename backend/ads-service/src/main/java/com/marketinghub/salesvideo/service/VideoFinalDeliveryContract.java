package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.media.Asset;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.dto.RequestSalesVideoPostProductionRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto.DeliveryPreparation;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Congela os bytes e a auditoria de um acabamento para preparar HLS sem nova geração. */
final class VideoFinalDeliveryContract {
  private static final Logger log = LoggerFactory.getLogger(VideoFinalDeliveryContract.class);

  /** Impede instanciar o montador determinístico do contrato. */
  private VideoFinalDeliveryContract() {}

  /** Valida a fonte persistida e proíbe mudar roteiro, legenda ou URL durante a recuperação. */
  static String prepare(
      SalesVideoJob source, RequestSalesVideoPostProductionRequest request, ObjectMapper mapper) {
    try {
      if (source.getStatus() != SalesVideoStatus.VIDEO_READY
          || !"MUSA_POST_PRODUCTION".equals(source.getProviderName())) {
        throw new IllegalArgumentException("Preparar HLS exige um acabamento final pronto.");
      }
      JsonNode metadata = mapper.readTree(source.getMetadataJson());
      DeliveryPreparation eligibility = inspect(source, metadata, mapper);
      String caption = eligibility.captionText();
      if (!"AVAILABLE".equals(eligibility.status())
          || !caption.equals(request.getCaptionText())
          || request.getSourceVideoUrl() != null
          || request.getVoiceOverScript() != null) {
        throw new IllegalArgumentException(
            "Recuperação HLS deve preservar integralmente o acabamento e seus textos.");
      }
      Map<String, Object> frozen =
          Map.of(
              "contractVersion",
              "VIDEO_FINAL_REUSE_V1",
              "jobId",
              source.getId(),
              "videoUrl",
              source.getAsset().getUrl(),
              "videoSha256",
              hash(source.getAsset(), mapper),
              "captionUrl",
              source.getVttAsset().getUrl(),
              "captionSha256",
              hash(source.getVttAsset(), mapper));
      ObjectNode result = ((ObjectNode) metadata).deepCopy();
      result.put("sourceJobId", source.getId());
      result.put("sourceVideoUrl", source.getAsset().getUrl());
      result.put("sourceProviderName", source.getProviderName());
      result.put("deliveryOnly", true);
      result.set("delivery_source", mapper.valueToTree(frozen));
      result.remove("hls_delivery");
      result.remove("cost_estimation");
      return mapper.writeValueAsString(result);
    } catch (Exception ex) {
      log.error(
          "Contrato de entrega HLS inválido; jobId={} tenant={}",
          source.getId(),
          source.getTenantId(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O acabamento precisa de MP4, VTT e hashes auditáveis; a recuperação não permite alterar conteúdo.",
          ex);
    }
  }

  /** Decide a disponibilidade pela integridade persistida, sem inferência de negócio na tela. */
  static DeliveryPreparation inspect(SalesVideoJob source, JsonNode metadata, ObjectMapper mapper) {
    if (source.getStatus() != SalesVideoStatus.VIDEO_READY
        || !"MUSA_POST_PRODUCTION".equals(source.getProviderName())) {
      return new DeliveryPreparation("UNAVAILABLE", source.getId(), null, null);
    }
    if (source.getStreamPlaybackUrl() != null && !source.getStreamPlaybackUrl().isBlank()) {
      return new DeliveryPreparation("READY", source.getId(), null, "Reprodução HLS já preparada.");
    }
    String caption = metadata.path("captionText").asText(metadata.at("/captions/text").asText());
    if (!(metadata instanceof ObjectNode)
        || caption.isBlank()
        || !metadata.path("has_audio").asBoolean()
        || !metadata.at("/captions/burned_in").asBoolean()
        || !auditable(source.getAsset(), mapper)
        || !auditable(source.getVttAsset(), mapper)) {
      return new DeliveryPreparation(
          "UNAVAILABLE",
          source.getId(),
          null,
          "O acabamento precisa de MP4, voz, legendas e hashes íntegros antes de preparar HLS.");
    }
    return new DeliveryPreparation(
        "AVAILABLE",
        source.getId(),
        caption,
        "Preparar reprodução preservando o vídeo, a voz e as legendas existentes.");
  }

  /** Confere os artefatos de upload sem transformar ausência de dados em disponibilidade. */
  private static boolean auditable(Asset asset, ObjectMapper mapper) {
    if (asset == null
        || asset.getUrl() == null
        || !asset.getUrl().matches("https?://[^\\s]+")
        || asset.getPayload() == null
        || asset.getPayload().isBlank()) return false;
    try {
      JsonNode payload = mapper.readTree(asset.getPayload());
      return payload != null && payload.at("/metadata/sha256").asText().matches("[a-f0-9]{64}");
    } catch (Exception ex) {
      log.error(
          "Falha ao conferir identidade de artefato para entrega; assetId={}", asset.getId(), ex);
      return false;
    }
  }

  /** Usa o hash dos bytes efetivamente recebidos no upload original. */
  private static String hash(Asset asset, ObjectMapper mapper) throws Exception {
    String hash = mapper.readTree(asset.getPayload()).at("/metadata/sha256").asText();
    if (!hash.matches("[a-f0-9]{64}"))
      throw new IllegalArgumentException("Asset sem hash de origem.");
    return hash;
  }
}
