package com.marketinghub.creative.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto.ProviderLicense;
import com.marketinghub.creative.dto.EditorialMediaEvidenceDto.AudioEvidence;
import com.marketinghub.creative.dto.EditorialMediaEvidenceDto.AudioSegment;
import com.marketinghub.creative.dto.EditorialMediaEvidenceDto.SpeechReceipt;
import com.marketinghub.creative.dto.EditorialMediaEvidenceDto.SpeechRequest;
import com.marketinghub.creative.dto.EditorialMediaEvidenceDto.SpeechResponse;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.dto.RequestSalesVideoPostProductionRequest;
import com.marketinghub.salesvideo.service.VideoNarrationRecovery;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Confere a origem de voz nativa OpenAI usando recibos existentes, sem síntese ou faturamento. */
@Component
public class BuiltInTtsEvidenceResolver {
  private static final Logger log = LoggerFactory.getLogger(BuiltInTtsEvidenceResolver.class);
  private static final Set<String> REQUEST_FIELDS =
      Set.of("model", "voice", "input", "instructions", "response_format", "speed");
  private final AssetRepository assets;
  private final SalesVideoJobRepository jobs;
  private final ObjectMapper json;
  private final JsonNode policy;

  /** Carrega a política versionada e conecta apenas portas de leitura da auditoria. */
  public BuiltInTtsEvidenceResolver(
      AssetRepository assets, SalesVideoJobRepository jobs, ObjectMapper json) {
    this.assets = assets;
    this.jobs = jobs;
    this.json = json;
    try (var stream =
        getClass().getResourceAsStream("/governance/openai-built-in-speech-v1.json")) {
      if (stream == null) throw new IOException("Política de voz nativa não encontrada");
      this.policy = json.readTree(stream);
    } catch (IOException ex) {
      log.error("Governança audiovisual: falha ao carregar política de voz nativa", ex);
      throw new IllegalStateException("Política de voz indisponível", ex);
    }
  }

  /** Revalida os segmentos incorporados contra o contrato de recuperação e os recibos originais. */
  public AudioEvidence resolve(
      SalesVideoJob source,
      SalesVideoJob finalJob,
      VideoProject project,
      JsonNode finalMetadata,
      int durationSeconds)
      throws JsonProcessingException {
    var blockers = new ArrayList<String>();
    var segments = new ArrayList<AudioSegment>();
    JsonNode audio = finalMetadata.path("audio");
    String model = audio.at("/review/model").asText();
    String voice = audio.at("/review/voice").asText();
    var disclosure = finalMetadata.path("synthetic_media_disclosure");
    boolean disclosed =
        disclosure.path("voice_synthetic").asBoolean(false)
            && disclosure.path("required").asBoolean(false)
            && !disclosure.path("text").asText().isBlank()
            && audio.path("ai_generated_disclosure").asBoolean(false);
    boolean nativeVoice =
        contains(policy.path("models"), model) && contains(policy.path("voices"), voice);
    if (!nativeVoice || !"OPENAI_TTS".equals(audio.at("/review/provider").asText()))
      blockers.add("Modelo ou voz não pertencem à política versionada de vozes nativas.");
    if (!disclosed) blockers.add("O arquivo final não comprova disclosure da voz sintética.");
    try {
      JsonNode metadata = read(finalJob.getMetadataJson());
      if (!metadata.path("tts_interactions").equals(finalMetadata.path("tts_interactions")))
        blockers.add("Recibos da mídia final divergem da auditoria do job.");
      var request = new RequestSalesVideoPostProductionRequest();
      request.setCaptionText(metadata.path("captionText").asText());
      request.setVoiceOverScript(metadata.path("voiceOverScript").asText());
      request.setTargetDurationSeconds(durationSeconds);
      JsonNode preserved =
          json.valueToTree(VideoNarrationRecovery.prepare(source, finalJob, request, json, assets));
      JsonNode parts = preserved.at("/preservedNarration/segments");
      if (!parts.isArray() || parts.isEmpty())
        blockers.add("A fala não possui segmentos auditáveis.");
      for (JsonNode part : parts) {
        AudioSegment segment = original(part, project, model, voice, blockers);
        if (segment != null) segments.add(segment);
      }
      if (parts.size() != finalMetadata.path("tts_interactions").size())
        blockers.add("Quantidade de segmentos não corresponde ao áudio incorporado.");
    } catch (ResponseStatusException ex) {
      log.warn(
          "Governança audiovisual: fala não recuperável. jobId={} projectId={}",
          finalJob.getId(),
          project.getId(),
          ex);
      blockers.add("A auditoria da fala incorporada é incompleta ou divergente: " + ex.getReason());
    }
    var license =
        nativeVoice && blockers.isEmpty()
            ? new ProviderLicense(
                policy.path("contractVersion").asText(),
                "OPENAI_TTS",
                true,
                policy.path("rightsUrl").asText(),
                Instant.parse(policy.path("verifiedAt").asText()))
            : null;
    return new AudioEvidence(
        "OPENAI_TTS",
        model,
        voice,
        nativeVoice,
        disclosed,
        String.join(" ", segments.stream().map(AudioSegment::text).toList()),
        "HISTORICAL_COST_UNCHANGED_NOT_RECONCILED",
        license,
        List.copyOf(segments),
        List.copyOf(blockers));
  }

  /**
   * Localiza o request que produziu exatamente o binário usado, preservando contexto e identidade.
   */
  private AudioSegment original(
      JsonNode part, VideoProject project, String model, String voice, List<String> blockers)
      throws JsonProcessingException {
    Asset asset = assets.findById(part.path("assetId").asLong()).orElse(null);
    if (asset == null) {
      blockers.add("Binário original de áudio ausente.");
      return null;
    }
    JsonNode metadata = read(asset.getPayload()).path("metadata");
    long originJobId = metadata.path("job_id").asLong();
    SalesVideoJob origin = originJobId > 0 ? jobs.findById(originJobId).orElse(null) : null;
    JsonNode originContext =
        origin == null ? json.createObjectNode() : read(origin.getMetadataJson());
    boolean sameOwner =
        origin != null
            && Objects.equals(origin.getTenantId(), project.getTenantId())
            && originContext.path("productId").asLong() == project.getProductId()
            && originContext.path("experimentId").asLong() == project.getExperimentId()
            && originContext.path("videoProjectId").asLong() == project.getId()
            && originContext.path("campaign_key").asText().equals(project.getCampaignKey());
    if (!sameOwner || !part.path("sha256").asText().equals(metadata.path("sha256").asText())) {
      blockers.add("Áudio original pertence a outro contexto ou hash.");
      return null;
    }
    for (JsonNode interaction : metadata.at("/provider_metadata/tts_interactions")) {
      JsonNode request = interaction.path("raw_request");
      JsonNode response = interaction.path("raw_response");
      if (!response.path("sha256").asText().equals(part.path("sha256").asText())) continue;
      var fields = new java.util.HashSet<String>();
      request.fieldNames().forEachRemaining(fields::add);
      boolean valid =
          "TEXT_TO_SPEECH".equals(interaction.path("interaction_type").asText())
              && "COMPLETED".equals(interaction.path("status").asText())
              && "OPENAI".equals(interaction.path("provider").asText())
              && policy.path("endpoint").asText().equals(interaction.path("endpoint").asText())
              && interaction.path("job_id").asLong() == originJobId
              && interaction.path("segment_index").asInt() == part.path("segmentIndex").asInt()
              && request.path("voice").isTextual()
              && voice.equals(request.path("voice").asText())
              && model.equals(request.path("model").asText())
              && model.equals(part.path("model").asText())
              && voice.equals(part.path("voice").asText())
              && part.path("text").asText().equals(request.path("input").asText())
              && REQUEST_FIELDS.containsAll(fields)
              && (!request.hasNonNull("instructions") || request.path("instructions").isTextual())
              && (!request.hasNonNull("response_format")
                  || request.path("response_format").isTextual())
              && (!request.hasNonNull("speed")
                  || request.path("speed").isNumber()
                      && Double.isFinite(request.path("speed").asDouble()))
              && response.path("bytes").asLong() > 0
              && response.path("bytes").asLong() == metadata.path("size_bytes").asLong()
              && response
                  .path("asset_file_name")
                  .asText()
                  .equals(metadata.path("file_name").asText());
      if (!valid) {
        blockers.add("Request original não comprova voz nativa, texto, ordem e resposta.");
        return null;
      }
      return new AudioSegment(
          asset.getId(),
          originJobId,
          part.path("segmentIndex").asInt(),
          asset.getUrl(),
          part.path("sha256").asText(),
          response.path("request_id").asText(null),
          sha256(request.path("input").asText()),
          part.path("durationSeconds").asDouble(),
          request.path("input").asText(),
          new SpeechReceipt(
              interaction.path("interaction_type").asText(),
              interaction.path("provider").asText(),
              interaction.path("endpoint").asText(),
              interaction.path("status").asText(),
              new SpeechRequest(
                  model,
                  voice,
                  request.path("input").asText(),
                  request.path("instructions").asText(null),
                  request.path("response_format").asText(null),
                  request.path("speed").isNumber() ? request.path("speed").doubleValue() : null),
              new SpeechResponse(
                  response.path("representation").asText(null),
                  response.path("asset_file_name").asText(),
                  response.path("content_type").asText(null),
                  response.path("bytes").asLong(),
                  response.path("sha256").asText(),
                  response.path("request_id").asText(null))));
    }
    blockers.add("Request e resposta originais do trecho de voz não foram localizados.");
    return null;
  }

  /** Reconhece somente itens explicitamente curados no recurso versionado. */
  private boolean contains(JsonNode values, String value) {
    for (JsonNode item : values) if (item.isTextual() && item.asText().equals(value)) return true;
    return false;
  }

  /** Lê auditoria persistida sem converter ausência em prova. */
  private JsonNode read(String value) throws JsonProcessingException {
    return value == null || value.isBlank() ? json.createObjectNode() : json.readTree(value);
  }

  /** Identifica o texto original sem expor o request completo na projeção comercial. */
  private String sha256(String value) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      log.error("Governança audiovisual: SHA-256 indisponível para texto de voz", ex);
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }
}
