package com.marketinghub.experiment.video.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.VideoProductionCycle;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.VideoProjectStatus;
import com.marketinghub.salesvideo.dto.JobCompletionRequest;
import com.marketinghub.salesvideo.dto.JobFailureRequest;
import com.marketinghub.salesvideo.service.SalesVideoCompletedRenderAssetSync;
import com.marketinghub.salesvideo.service.SalesVideoProductionCostCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Reconcilia o resultado final do Estúdio com o ativo, o ciclo e o projeto comercial de origem. */
@Service
@Slf4j
public class ExperimentVideoAssetJobSyncService implements SalesVideoCompletedRenderAssetSync {
  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
  private static final String FINALIZATION_PROVIDER = "MUSA_POST_PRODUCTION";
  private static final int MAX_LINEAGE_DEPTH = 12;
  private static final String NO_AUDIO_REJECTION_REASON =
      "Reprovado na qualidade: vídeo final não possui faixa de áudio. Gerar novamente com voz, mixagem ou trilha audível antes da aprovação humana.";

  private final ExperimentVideoAssetRepository repository;
  private final SalesVideoProductionCostCalculator costCalculator;
  private final ExperimentRepository experimentRepository;
  private final VideoProductionCycleRepository cycleRepository;
  private final VideoProjectRepository projectRepository;

  /** Inicializa a sincronizacao com persistencia dos ativos e calculo oficial de custo. */
  public ExperimentVideoAssetJobSyncService(
      ExperimentVideoAssetRepository repository,
      SalesVideoProductionCostCalculator costCalculator,
      ExperimentRepository experimentRepository,
      VideoProductionCycleRepository cycleRepository,
      VideoProjectRepository projectRepository) {
    this.repository = repository;
    this.costCalculator = costCalculator;
    this.experimentRepository = experimentRepository;
    this.cycleRepository = cycleRepository;
    this.projectRepository = projectRepository;
  }

  /** Propaga o asset final, poster, duracao, custo e metadata para os videos vinculados ao job. */
  @Override
  public void syncCompletedRender(
      SalesVideoJob job, JobCompletionRequest request, Integer durationSeconds, String resolution) {
    if (job == null || job.getId() == null) {
      return;
    }
    List<ExperimentVideoAsset> videoAssets = repository.findBySalesVideoJobId(job.getId());
    GovernedStudioContext context = resolveGovernedStudioContext(job).orElse(null);
    if (videoAssets.isEmpty()) {
      videoAssets =
          createFinalExperimentAsset(job, request, durationSeconds, context).stream().toList();
    }
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
    concludeGovernedStudioCycle(job, context);
  }

  /**
   * Cria o vínculo comercial que faltava quando um projeto governado conclui a pós-produção sem ter
   * nascido de um ativo de experimento planejado.
   */
  private Optional<ExperimentVideoAsset> createFinalExperimentAsset(
      SalesVideoJob job,
      JobCompletionRequest request,
      Integer durationSeconds,
      GovernedStudioContext context) {
    if (context == null
        || job.getJobType() != SalesVideoJobType.POST_PRODUCTION
        || job.getStatus() != SalesVideoStatus.VIDEO_READY
        || !FINALIZATION_PROVIDER.equalsIgnoreCase(job.getProviderName())
        || job.getAsset() == null) {
      return Optional.empty();
    }
    Optional<VideoProject> project = projectRepository.findById(context.videoProjectId());
    if (project.isEmpty()
        || !sameTenant(project.get(), job)
        || !context.experimentId().equals(project.get().getExperimentId())) {
      log.warn(
          "Pós-produção governada não foi vinculada ao experimento por contexto divergente. jobId={} projectId={} experimentId={}",
          job.getId(),
          context.videoProjectId(),
          context.experimentId());
      return Optional.empty();
    }
    return experimentRepository
        .findById(context.experimentId())
        .map(
            experiment ->
                ExperimentVideoAsset.builder()
                    .experiment(experiment)
                    .slot(resolveSlot(project.get()))
                    .objective(
                        limit(
                            requiredText(project.get().getObjective(), project.get().getTitle()),
                            512))
                    .primaryMetric(
                        limit(
                            requiredText(
                                project.get().getPrimaryMetric(), "VIDEO_75_AND_CTA_CLICK"),
                            191))
                    .script(
                        job.getScript() != null
                            ? job.getScript().getScriptText()
                            : project.get().getScriptText())
                    .prompt(project.get().getScenePlan())
                    .provider(FINALIZATION_PROVIDER)
                    .model(resolveModel(request))
                    .status(ExperimentVideoStatus.READY)
                    .assetUrl(job.getAsset().getUrl())
                    .hlsPlaybackUrl(resolveHlsPlaybackUrl(job, request).orElse(null))
                    .thumbnailUrl(
                        job.getPosterAsset() != null ? job.getPosterAsset().getUrl() : null)
                    .durationSeconds(durationSeconds)
                    .hasAudio(resolveHasAudio(job, request))
                    .aspectRatio(resolveAspectRatio(project.get()))
                    .visualSourceType("STUDIO_GOVERNED_FINAL")
                    .visualSourceKey(
                        "studio-project-%d-final-job-%d"
                            .formatted(project.get().getId(), job.getId()))
                    .visualSourceDescription(
                        "Peça final produzida pelo Estúdio a partir do ciclo governado #"
                            + context.videoProductionCycleId()
                            + ".")
                    .requestJson(job.getAuditSnapshotJson())
                    .responseJson(
                        request != null ? request.getMetadataJson() : job.getMetadataJson())
                    .cost(resolveCost(job, request, durationSeconds))
                    .audioCost(resolveAudioCost(job, request).orElse(null))
                    .reviewStatus(ExperimentVideoReviewStatus.PENDING)
                    .requiredForRelease(true)
                    .salesVideoProfile(job.getProfile())
                    .salesVideoJob(job)
                    .asset(job.getAsset())
                    .build());
  }

  /** Encerra o ciclo em revisão e move o projeto sem aprovar humanamente a peça. */
  private void concludeGovernedStudioCycle(SalesVideoJob job, GovernedStudioContext context) {
    if (context == null || job.getStatus() != SalesVideoStatus.VIDEO_READY) {
      return;
    }
    cycleRepository
        .findById(context.videoProductionCycleId())
        .filter(cycle -> context.videoProjectId().equals(cycle.getVideoProjectId()))
        .filter(cycle -> context.experimentId().equals(cycle.getExperimentId()))
        .ifPresent(
            cycle -> {
              cycle.setSalesVideoJobId(job.getId());
              cycle.setStatus("VIDEO_READY_FOR_REVIEW");
              cycle.setUpdatedAt(Instant.now());
              cycleRepository.save(cycle);
            });
    projectRepository
        .findById(context.videoProjectId())
        .filter(project -> context.experimentId().equals(project.getExperimentId()))
        .ifPresent(
            project -> {
              project.setStatus(VideoProjectStatus.READY_FOR_REVIEW);
              project.setUpdatedBy("Marketing Hub");
              projectRepository.save(project);
            });
  }

  /**
   * Recupera ciclo, projeto e experimento pela linhagem, inclusive em jobs legados de acabamento.
   */
  private Optional<GovernedStudioContext> resolveGovernedStudioContext(SalesVideoJob job) {
    Long cycleId = null;
    Long projectId = null;
    Long experimentId = null;
    SalesVideoJob current = job;
    Set<Long> visited = new HashSet<>();
    for (int depth = 0; current != null && depth < MAX_LINEAGE_DEPTH; depth++) {
      if (current.getId() != null && !visited.add(current.getId())) {
        break;
      }
      JsonNode metadata = readMetadata(current);
      cycleId = firstPositive(cycleId, longField(metadata, "videoProductionCycleId"));
      projectId =
          firstPositive(
              projectId,
              longField(metadata, "videoProjectId"),
              longField(metadata, "studio_project_id"));
      experimentId = firstPositive(experimentId, longField(metadata, "experimentId"));
      current = current.getRetryOfJob();
    }
    if (cycleId == null) {
      return Optional.empty();
    }
    Optional<VideoProductionCycle> cycle = cycleRepository.findById(cycleId);
    if (cycle.isPresent()) {
      projectId = firstPositive(projectId, cycle.get().getVideoProjectId());
      experimentId = firstPositive(experimentId, cycle.get().getExperimentId());
    }
    if (projectId == null || experimentId == null) {
      return Optional.empty();
    }
    return Optional.of(new GovernedStudioContext(cycleId, projectId, experimentId));
  }

  /** Lê metadata de uma tentativa sem promover JSON inválido a contexto comercial. */
  private JsonNode readMetadata(SalesVideoJob job) {
    if (job == null || job.getMetadataJson() == null || job.getMetadataJson().isBlank()) {
      return OBJECT_MAPPER.missingNode();
    }
    try {
      return OBJECT_MAPPER.readTree(job.getMetadataJson());
    } catch (JsonProcessingException ex) {
      log.warn(
          "Metadata inválido ignorado ao resolver linhagem de vídeo. jobId={}", job.getId(), ex);
      return OBJECT_MAPPER.missingNode();
    }
  }

  /** Seleciona o primeiro identificador positivo disponível. */
  private Long firstPositive(Long... values) {
    for (Long value : values) {
      if (value != null && value > 0) {
        return value;
      }
    }
    return null;
  }

  /** Lê identificador numérico positivo do metadata. */
  private Long longField(JsonNode metadata, String fieldName) {
    JsonNode value = metadata.path(fieldName);
    return value.canConvertToLong() && value.asLong() > 0 ? value.asLong() : null;
  }

  /** Garante que o job não atravesse a fronteira de tenant do projeto. */
  private boolean sameTenant(VideoProject project, SalesVideoJob job) {
    return project.getTenantId() != null && project.getTenantId().equals(job.getTenantId());
  }

  /** Resolve o papel do vídeo pela intenção persistida no projeto, sem depender do ID do Vega. */
  private ExperimentVideoSlot resolveSlot(VideoProject project) {
    String channel = Optional.ofNullable(project.getTargetChannel()).orElse("").toUpperCase();
    String stage = Optional.ofNullable(project.getFunnelStage()).orElse("").toUpperCase();
    if (channel.contains("INSTAGRAM")
        || channel.contains("FACEBOOK")
        || channel.contains("META")
        || channel.contains("TIKTOK")
        || stage.contains("AD")) {
      return ExperimentVideoSlot.AD;
    }
    return ExperimentVideoSlot.LANDING_HERO;
  }

  /** Converte o formato editorial do projeto em proporção de mídia. */
  private String resolveAspectRatio(VideoProject project) {
    String format = Optional.ofNullable(project.getFormat()).orElse("").toUpperCase();
    return format.contains("9_16") || format.contains("VERTICAL") ? "9:16" : null;
  }

  /** Resolve o modelo de áudio efetivamente registrado no acabamento. */
  private String resolveModel(JobCompletionRequest request) {
    if (request != null && request.getMetadataJson() != null) {
      try {
        String model =
            OBJECT_MAPPER
                .readTree(request.getMetadataJson())
                .path("audio")
                .path("review")
                .path("model")
                .asText();
        if (model != null && !model.isBlank()) {
          return limit(model, 128);
        }
      } catch (JsonProcessingException ex) {
        log.warn("Metadata inválido impediu identificar o modelo final de áudio.", ex);
      }
    }
    return FINALIZATION_PROVIDER;
  }

  /** Usa o custo informado e mantém estimativa somente quando o provider não o reportou. */
  private BigDecimal resolveCost(
      SalesVideoJob job, JobCompletionRequest request, Integer durationSeconds) {
    if (request != null && request.getCostUsd() != null) {
      return request.getCostUsd();
    }
    return costCalculator.estimateUsd(
        job.getProviderName(), job.getProviderName(), durationSeconds, "720p");
  }

  /** Preenche texto obrigatório com fallback persistido. */
  private String requiredText(String preferred, String fallback) {
    return preferred != null && !preferred.isBlank() ? preferred.trim() : fallback.trim();
  }

  /** Limita campos de resumo aos tamanhos canônicos do banco sem truncar artefatos extensos. */
  private String limit(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }

  /** Identifica o contrato governado recuperado da linhagem do job final. */
  private record GovernedStudioContext(
      Long videoProductionCycleId, Long videoProjectId, Long experimentId) {}

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
