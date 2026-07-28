package com.marketinghub.experiment.video.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.dto.JobCompletionRequest;
import com.marketinghub.salesvideo.dto.JobFailureRequest;
import com.marketinghub.salesvideo.service.SalesVideoCompletedRenderAssetSync;
import com.marketinghub.salesvideo.service.SalesVideoProductionCostCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Atualiza ativos de video de experimento a partir do resultado final de um job SalesVideo. */
@Service
@Slf4j
public class ExperimentVideoAssetJobSyncService implements SalesVideoCompletedRenderAssetSync {
  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
  private static final String NO_AUDIO_REJECTION_REASON =
      "Reprovado na qualidade: vídeo final não possui faixa de áudio. Gerar novamente com voz, mixagem ou trilha audível antes da aprovação humana.";

  private final ExperimentVideoAssetRepository repository;
  private final SalesVideoProductionCostCalculator costCalculator;

  /** Inicializa a sincronizacao com persistencia dos ativos e calculo oficial de custo. */
  public ExperimentVideoAssetJobSyncService(
      ExperimentVideoAssetRepository repository,
      SalesVideoProductionCostCalculator costCalculator) {
    this.repository = repository;
    this.costCalculator = costCalculator;
  }

  /** Propaga o asset final, poster, duracao, custo e metadata para os videos vinculados ao job. */
  @Override
  public void syncCompletedRender(
      SalesVideoJob job, JobCompletionRequest request, Integer durationSeconds, String resolution) {
    if (job == null || job.getId() == null) {
      return;
    }
    List<ExperimentVideoAsset> videoAssets = repository.findBySalesVideoJobId(job.getId());
    if (videoAssets.isEmpty()) {
      return;
    }
    BigDecimal costUsd =
        Optional.ofNullable(request.getCostUsd())
            .orElseGet(
                () ->
                    costCalculator.estimateUsd(
                        job.getProviderName(), job.getProviderName(), durationSeconds, resolution));
    for (ExperimentVideoAsset videoAsset : videoAssets) {
      applyCompletedRender(videoAsset, job, request, durationSeconds, costUsd);
    }
    repository.saveAll(videoAssets);
  }

  /** Propaga falha definitiva de render para os videos de experimento vinculados ao job. */
  @Override
  public void syncFailedRender(SalesVideoJob job, JobFailureRequest request) {
    if (job == null || job.getId() == null) {
      return;
    }
    List<ExperimentVideoAsset> videoAssets = repository.findBySalesVideoJobId(job.getId());
    if (videoAssets.isEmpty()) {
      return;
    }
    for (ExperimentVideoAsset videoAsset : videoAssets) {
      applyFailedRender(videoAsset, job, request);
    }
    repository.saveAll(videoAssets);
  }

  /** Aplica os campos auditaveis do render concluido em um ativo de experimento. */
  private void applyCompletedRender(
      ExperimentVideoAsset videoAsset,
      SalesVideoJob job,
      JobCompletionRequest request,
      Integer durationSeconds,
      BigDecimal costUsd) {
    if (job.getAsset() != null) {
      videoAsset.setAsset(job.getAsset());
      videoAsset.setAssetUrl(job.getAsset().getUrl());
    }
    resolveHlsPlaybackUrl(job, request).ifPresent(videoAsset::setHlsPlaybackUrl);
    if (job.getPosterAsset() != null) {
      videoAsset.setThumbnailUrl(job.getPosterAsset().getUrl());
    }
    if (durationSeconds != null) {
      videoAsset.setDurationSeconds(durationSeconds);
    }
    Boolean hasAudio = resolveHasAudio(job, request);
    if (hasAudio != null) {
      videoAsset.setHasAudio(hasAudio);
    }
    if (costUsd != null) {
      videoAsset.setCost(costUsd);
    }
    resolveAudioCost(job, request).ifPresent(videoAsset::setAudioCost);
    videoAsset.setResponseJson(request.getMetadataJson());
    videoAsset.setStatus(ExperimentVideoStatus.READY);
    if (Boolean.FALSE.equals(hasAudio)) {
      rejectWithoutAudio(videoAsset);
    }
  }

  /** Extrai custo de áudio separado do metadata enviado pelo worker quando existir. */
  private Optional<BigDecimal> resolveAudioCost(SalesVideoJob job, JobCompletionRequest request) {
    if (request == null
        || request.getMetadataJson() == null
        || request.getMetadataJson().isBlank()) {
      return Optional.empty();
    }
    try {
      JsonNode metadata = OBJECT_MAPPER.readTree(request.getMetadataJson());
      return firstDecimal(metadata, "audioCostUsd", "audio_cost_usd", "audioCost", "audio_cost");
    } catch (JsonProcessingException ex) {
      log.warn(
          "Falha ao extrair custo de audio do metadata do video. classe={} operacao=resolveAudioCost jobId={}",
          getClass().getSimpleName(),
          job != null ? job.getId() : null,
          ex);
      return Optional.empty();
    }
  }

  /** Extrai a playlist HLS final do job ou do payload auditável enviado pelo worker. */
  private Optional<String> resolveHlsPlaybackUrl(SalesVideoJob job, JobCompletionRequest request) {
    return firstHlsUrl(
        job != null ? job.getStreamPlaybackUrl() : null,
        request != null ? request.getStreamPlaybackUrl() : null,
        textMetadataField(request, "hlsPlaybackUrl"),
        textMetadataField(request, "hls_playback_url"),
        textMetadataField(request, "streamPlaybackUrl"),
        textMetadataField(request, "stream_playback_url"));
  }

  /** Seleciona a primeira URL HLS válida sem promover MP4 como contrato de PDE. */
  private Optional<String> firstHlsUrl(String... candidates) {
    for (String candidate : candidates) {
      if (candidate != null && candidate.trim().contains(".m3u8")) {
        return Optional.of(candidate.trim());
      }
    }
    return Optional.empty();
  }

  /** Lê um campo textual do metadata de conclusão quando o worker envia aliases diferentes. */
  private String textMetadataField(JobCompletionRequest request, String fieldName) {
    if (request == null
        || request.getMetadataJson() == null
        || request.getMetadataJson().isBlank()) {
      return null;
    }
    try {
      JsonNode metadata = OBJECT_MAPPER.readTree(request.getMetadataJson());
      JsonNode value = metadata.get(fieldName);
      return value != null && value.isTextual() ? value.asText() : null;
    } catch (JsonProcessingException ex) {
      log.warn(
          "Falha ao extrair URL HLS do metadata do video. classe={} operacao=textMetadataField fieldName={}",
          getClass().getSimpleName(),
          fieldName,
          ex);
      return null;
    }
  }

  /** Lê valores decimais aceitos para compatibilidade com workers diferentes. */
  private Optional<BigDecimal> firstDecimal(JsonNode metadata, String... fieldNames) {
    for (String fieldName : fieldNames) {
      JsonNode value = metadata.get(fieldName);
      if (value != null && value.isNumber()) {
        return Optional.of(value.decimalValue());
      }
      if (value != null && value.isTextual() && isDecimalText(value.asText())) {
        return Optional.of(new BigDecimal(value.asText()));
      }
    }
    return Optional.empty();
  }

  /** Valida texto decimal simples antes da conversão para evitar exceção em metadata externo. */
  private boolean isDecimalText(String value) {
    return value != null && value.trim().matches("-?\\d+(\\.\\d+)?");
  }

  /** Bloqueia uso comercial quando a qualidade confirma ausência de áudio. */
  private void rejectWithoutAudio(ExperimentVideoAsset videoAsset) {
    videoAsset.setReviewStatus(ExperimentVideoReviewStatus.REJECTED);
    videoAsset.setRejectionReason(NO_AUDIO_REJECTION_REASON);
    videoAsset.setReviewedBy("Marketing Hub Quality Gate");
    videoAsset.setReviewedAt(Instant.now());
  }

  /** Extrai do metadata auditável se o arquivo final possui faixa de áudio. */
  private Boolean resolveHasAudio(SalesVideoJob job, JobCompletionRequest request) {
    if (request == null
        || request.getMetadataJson() == null
        || request.getMetadataJson().isBlank()) {
      return null;
    }
    try {
      JsonNode metadata = OBJECT_MAPPER.readTree(request.getMetadataJson());
      return firstBoolean(metadata, "hasAudio", "has_audio", "audioPresent", "audio_present")
          .or(() -> audioStreamCount(metadata))
          .orElse(null);
    } catch (JsonProcessingException ex) {
      log.warn(
          "Falha ao extrair qualidade de audio do metadata do video. classe={} operacao=resolveHasAudio jobId={}",
          getClass().getSimpleName(),
          job != null ? job.getId() : null,
          ex);
      return null;
    }
  }

  /** Lê flags booleanas aceitas para compatibilidade com workers diferentes. */
  private Optional<Boolean> firstBoolean(JsonNode metadata, String... fieldNames) {
    for (String fieldName : fieldNames) {
      JsonNode value = metadata.get(fieldName);
      if (value != null && value.isBoolean()) {
        return Optional.of(value.asBoolean());
      }
    }
    return Optional.empty();
  }

  /** Interpreta contagens de streams de áudio vindas de ffprobe ou metadata normalizado. */
  private Optional<Boolean> audioStreamCount(JsonNode metadata) {
    JsonNode audioStreams = metadata.get("audio_streams");
    if (audioStreams == null) {
      audioStreams = metadata.get("audioStreams");
    }
    if (audioStreams != null && audioStreams.isNumber()) {
      return Optional.of(audioStreams.asInt() > 0);
    }
    JsonNode streams = metadata.get("streams");
    if (streams != null && streams.isArray()) {
      for (JsonNode stream : streams) {
        JsonNode codecType = stream.get("codec_type");
        if (codecType != null && "audio".equalsIgnoreCase(codecType.asText())) {
          return Optional.of(true);
        }
      }
      return Optional.of(false);
    }
    return Optional.empty();
  }

  /** Registra a causa da falha no ativo para a tela não permanecer como gerando. */
  private void applyFailedRender(
      ExperimentVideoAsset videoAsset, SalesVideoJob job, JobFailureRequest request) {
    videoAsset.setStatus(ExperimentVideoStatus.FAILED);
    videoAsset.setResponseJson(failureSnapshot(job, request));
  }

  /** Monta um JSON simples com a causa operacional da falha do render. */
  private String failureSnapshot(SalesVideoJob job, JobFailureRequest request) {
    String message = request != null ? request.getMessage() : null;
    String failureCode =
        request != null && request.getFailureCode() != null
            ? request.getFailureCode()
            : job.getFailureCode();
    String failureDetail =
        request != null && request.getFailureDetail() != null
            ? request.getFailureDetail()
            : job.getFailureDetail();
    try {
      return OBJECT_MAPPER.writeValueAsString(
          new FailureSnapshot("VIDEO_FAILED", job.getId(), failureCode, message, failureDetail));
    } catch (JsonProcessingException ex) {
      log.warn(
          "Falha ao montar snapshot de falha do video. classe={} operacao=failureSnapshot jobId={}",
          getClass().getSimpleName(),
          job != null ? job.getId() : null,
          ex);
      return "{\"status\":\"VIDEO_FAILED\",\"jobId\":%d}".formatted(job.getId());
    }
  }

  /** Snapshot persistido para explicar a falha operacional na tela do experimento. */
  private record FailureSnapshot(
      String status, Long jobId, String failureCode, String message, String failureDetail) {}
}
