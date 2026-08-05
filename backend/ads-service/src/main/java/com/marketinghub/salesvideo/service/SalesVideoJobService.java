package com.marketinghub.salesvideo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobEventRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoScriptRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.*;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.mapper.SalesVideoMapper;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Contém as regras de negócio relativas aos jobs do módulo de vídeo. */
@Component
public class SalesVideoJobService {
  private static final Logger log = LoggerFactory.getLogger(SalesVideoJobService.class);
  private static final int MAX_PAGE_SIZE = 200;
  private static final int MAX_MONTAGE_DURATION_SECONDS = 600;
  private static final int MAX_CINEMATIC_SCENES = 12;
  private static final String SHORT_DURATION_FAILURE_CODE = "RENDER_DURATION_SHORT";

  private final SalesVideoJobRepository jobRepository;
  private final SalesVideoJobEventRepository eventRepository;
  private final SalesVideoProfileRepository profileRepository;
  private final SalesVideoScriptRepository scriptRepository;
  private final AssetRepository assetRepository;
  private final SalesVideoReprocessPolicy reprocessPolicy;
  private final SalesVideoCompletedRenderAssetSync completedRenderAssetSync;
  private final SalesVideoJobCostMetadataService jobCostMetadataService;
  private final ObjectMapper objectMapper;
  private final VideoProjectRepository videoProjectRepository;
  private final StudioCostLedgerService studioCostLedgerService;

  /** Cria o serviço com repositórios, política de reprocessamento e porta de sincronizacao. */
  @Autowired
  public SalesVideoJobService(
      SalesVideoJobRepository jobRepository,
      SalesVideoJobEventRepository eventRepository,
      SalesVideoProfileRepository profileRepository,
      SalesVideoScriptRepository scriptRepository,
      AssetRepository assetRepository,
      SalesVideoReprocessPolicy reprocessPolicy,
      SalesVideoCompletedRenderAssetSync completedRenderAssetSync,
      SalesVideoJobCostMetadataService jobCostMetadataService,
      VideoProjectRepository videoProjectRepository,
      StudioCostLedgerService studioCostLedgerService,
      ObjectMapper objectMapper) {
    this.jobRepository = jobRepository;
    this.eventRepository = eventRepository;
    this.profileRepository = profileRepository;
    this.scriptRepository = scriptRepository;
    this.assetRepository = assetRepository;
    this.reprocessPolicy = reprocessPolicy;
    this.completedRenderAssetSync = completedRenderAssetSync;
    this.jobCostMetadataService = jobCostMetadataService;
    this.videoProjectRepository = videoProjectRepository;
    this.studioCostLedgerService = studioCostLedgerService;
    this.objectMapper = objectMapper;
  }

  /** Inicializa testes legados que não exercitam o ledger comercial do Estúdio. */
  SalesVideoJobService(
      SalesVideoJobRepository jobRepository,
      SalesVideoJobEventRepository eventRepository,
      SalesVideoProfileRepository profileRepository,
      SalesVideoScriptRepository scriptRepository,
      AssetRepository assetRepository,
      SalesVideoReprocessPolicy reprocessPolicy,
      SalesVideoCompletedRenderAssetSync completedRenderAssetSync,
      SalesVideoJobCostMetadataService jobCostMetadataService,
      ObjectMapper objectMapper) {
    this(
        jobRepository,
        eventRepository,
        profileRepository,
        scriptRepository,
        assetRepository,
        reprocessPolicy,
        completedRenderAssetSync,
        jobCostMetadataService,
        null,
        null,
        objectMapper);
  }

  @Transactional
  public SalesVideoJob createJob(
      SalesVideoProfile profile,
      SalesVideoScript script,
      SalesVideoJobType jobType,
      SalesVideoProviderFamily providerFamily,
      String providerName,
      String requestedBy,
      SalesVideoExecutionMode executionMode) {
    SalesVideoStatus initialStatus = initialStatus(jobType);
    SalesVideoJob job =
        SalesVideoJob.builder()
            .profile(profile)
            .tenantId(profile != null ? profile.getTenantId() : TenantContextHolder.currentTenant())
            .script(script)
            .jobType(jobType)
            .providerFamily(providerFamily)
            .executionMode(executionMode != null ? executionMode : SalesVideoExecutionMode.TEST)
            .providerName(providerName)
            .status(initialStatus)
            .requestedBy(requestedBy)
            .requestedAt(Instant.now())
            .build();
    SalesVideoJob saved = jobRepository.save(job);
    registerEvent(saved, SalesVideoJobEventType.CREATED, null, initialStatus, "Job criado", null);
    maybeUpdateProfileStatus(saved, initialStatus);
    return saved;
  }

  @Transactional(readOnly = true)
  public List<SalesVideoJobDto> findJobs(
      SalesVideoProviderFamily providerFamily,
      SalesVideoStatus status,
      SalesVideoJobType jobType,
      int limit) {
    Specification<SalesVideoJob> spec = Specification.where(null);
    if (providerFamily != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("providerFamily"), providerFamily));
    }
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (jobType != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("jobType"), jobType));
    }
    Pageable pageable =
        PageRequest.of(
            0,
            Math.max(1, Math.min(limit <= 0 ? 50 : limit, MAX_PAGE_SIZE)),
            Sort.by(Sort.Direction.ASC, "requestedAt"));
    return jobRepository.findAll(spec, pageable).stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public SalesVideoJobDto getJob(Long jobId) {
    return toDto(loadJob(jobId));
  }

  @Transactional(readOnly = true)
  public List<SalesVideoJobEventDto> getJobEvents(Long jobId) {
    ensureJobExists(jobId);
    return eventRepository.findByJobIdOrderByCreatedAtAsc(jobId).stream()
        .map(SalesVideoMapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<SalesVideoJobDto> listJobsByProfile(Long profileId) {
    ensureProfileAccessible(profileId);
    return jobRepository.findByProfileIdOrderByRequestedAtDesc(profileId).stream()
        .map(this::toDto)
        .toList();
  }

  /** Lista todos os jobs de vídeo de um produto para gestão e totalização de custo. */
  @Transactional(readOnly = true)
  public List<SalesVideoJobDto> listJobsByProduct(Long productId) {
    String tenantId = TenantContextHolder.requireTenant();
    return jobRepository
        .findByProfileProductIdAndTenantIdOrderByRequestedAtDesc(productId, tenantId)
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public SalesVideoJobDto claimJob(Long jobId, JobClaimRequest request) {
    SalesVideoJob job = loadJob(jobId);
    if (job.getStartedAt() == null) {
      job.setStartedAt(Instant.now());
    }
    jobRepository.save(job);
    registerEvent(
        job,
        SalesVideoJobEventType.CLAIMED,
        job.getStatus(),
        job.getStatus(),
        "claim por " + request.getWorkerId(),
        request.getMessage());
    return toDto(job);
  }

  /** Sincroniza custo de vídeo quando o job pertence a um projeto comercial auditável. */
  private void syncStudioCostLedger(
      SalesVideoJob job, BigDecimal costUsd, boolean providerReported) {
    if (videoProjectRepository == null || studioCostLedgerService == null) {
      return;
    }
    Long projectId = readStudioProjectId(job.getMetadataJson());
    if (projectId == null) {
      return;
    }
    videoProjectRepository
        .findById(projectId)
        .filter(project -> project.getProductId() != null && project.getCommercialPlanId() != null)
        .ifPresent(
            project ->
                studioCostLedgerService.recordVideo(
                    job.getId(),
                    project.getProductId(),
                    project.getCommercialPlanId(),
                    project.getExperimentId(),
                    job.getProviderName(),
                    job.getProviderName(),
                    job.getStatus().name(),
                    costUsd,
                    providerReported,
                    job.getStartedAt(),
                    job.getFinishedAt()));
  }

  /** Extrai o projeto do Estúdio preservado no contrato do job. */
  private Long readStudioProjectId(String metadataJson) {
    if (!StringUtils.hasText(metadataJson)) {
      return null;
    }
    try {
      JsonNode value = objectMapper.readTree(metadataJson).path("studio_project_id");
      return value.canConvertToLong() && value.asLong() > 0 ? value.asLong() : null;
    } catch (JsonProcessingException ex) {
      log.error("Falha ao ler projeto do Estúdio para ledger; metadata={}", metadataJson, ex);
      return null;
    }
  }

  @Transactional
  public SalesVideoJobDto heartbeat(Long jobId, JobHeartbeatRequest request) {
    SalesVideoJob job = loadJob(jobId);
    registerEvent(
        job,
        SalesVideoJobEventType.HEARTBEAT,
        job.getStatus(),
        job.getStatus(),
        request.getMessage(),
        request.getDetailsJson());
    return toDto(job);
  }

  @Transactional
  public SalesVideoJobDto progress(Long jobId, JobProgressRequest request) {
    SalesVideoJob job = loadJob(jobId);
    if (request.getProgressPercent() != null) {
      job.setProgressPercent(Math.max(0, Math.min(100, request.getProgressPercent())));
    }
    SalesVideoStatus oldStatus = job.getStatus();
    if (request.getStatus() != null && request.getStatus() != job.getStatus()) {
      job.setStatus(request.getStatus());
      maybeUpdateProfileStatus(job, request.getStatus());
    }
    jobRepository.save(job);
    registerEvent(
        job,
        SalesVideoJobEventType.PROGRESS,
        oldStatus,
        job.getStatus(),
        request.getMessage(),
        request.getDetailsJson());
    return toDto(job);
  }

  /** Finaliza o job, aplica gates de duração e encadeia a pós-produção cinematográfica. */
  @Transactional
  public SalesVideoJobDto complete(Long jobId, JobCompletionRequest request) {
    SalesVideoJob job = loadJob(jobId);
    String requestedJobMetadata = job.getMetadataJson();
    SalesVideoStatus previous = job.getStatus();
    SalesVideoStatus finalStatus =
        Optional.ofNullable(request.getStatus()).orElse(defaultCompletionStatus(job.getJobType()));
    Integer durationSeconds = resolveDurationSeconds(job, request);
    DurationValidation durationValidation = validateRenderDuration(job, durationSeconds);
    if (!durationValidation.valid()) {
      finalStatus = SalesVideoStatus.VIDEO_FAILED;
      job.setFailureCode(SHORT_DURATION_FAILURE_CODE);
      job.setFailureDetail(durationValidation.message());
    }
    BigDecimal explicitCostUsd = request.getCostUsd();
    String completionMetadataJson =
        mergeRequestedAndCompletionMetadata(requestedJobMetadata, request.getMetadataJson());
    BigDecimal resolvedCostUsd =
        jobCostMetadataService.resolveCostUsd(job, completionMetadataJson, explicitCostUsd);
    request.setCostUsd(resolvedCostUsd);
    String enrichedMetadataJson =
        jobCostMetadataService.enrichMetadataJson(job, completionMetadataJson, explicitCostUsd);
    job.setStatus(finalStatus);
    job.setFinishedAt(Instant.now());
    job.setProviderJobId(request.getProviderJobId());
    job.setStreamPlaybackUrl(normalizeStreamPlaybackUrl(request.getStreamPlaybackUrl()));
    job.setMetadataJson(enrichedMetadataJson);
    request.setMetadataJson(enrichedMetadataJson);
    attachAsset(job::setAsset, request.getAssetId());
    attachAsset(job::setPosterAsset, request.getPosterAssetId());
    attachAsset(job::setVttAsset, request.getVttAssetId());
    SalesVideoScript persistedScript = maybePersistScriptResult(job, request.getScriptResult());
    if (persistedScript != null) {
      job.setScript(persistedScript);
    }
    jobRepository.save(job);
    syncStudioCostLedger(job, resolvedCostUsd, explicitCostUsd != null);
    syncExperimentVideoAsset(job, request, durationSeconds);
    syncFailedExperimentVideoAsset(job, completionFailureRequest(request));
    maybeUpdateProfileStatus(job, finalStatus);
    registerEvent(
        job,
        SalesVideoJobEventType.COMPLETED,
        previous,
        finalStatus,
        completionMessage(request, durationValidation),
        completionDetails(request, durationValidation));
    enqueuePremiumFinalization(job, requestedJobMetadata);
    return toDto(job);
  }

  /** Preserva o contrato comercial solicitado e acrescenta o resultado técnico do provider. */
  private String mergeRequestedAndCompletionMetadata(
      String requestedMetadataJson, String completionMetadataJson) {
    if (!StringUtils.hasText(requestedMetadataJson)) {
      return completionMetadataJson;
    }
    if (!StringUtils.hasText(completionMetadataJson)) {
      return requestedMetadataJson;
    }
    try {
      JsonNode requested = objectMapper.readTree(requestedMetadataJson);
      JsonNode completion = objectMapper.readTree(completionMetadataJson);
      if (!requested.isObject() || !completion.isObject()) {
        return completionMetadataJson;
      }
      ObjectNode merged = ((ObjectNode) requested).deepCopy();
      completion.fields().forEachRemaining(entry -> merged.set(entry.getKey(), entry.getValue()));
      List.of(
              "commercial_goal",
              "generation_strategy",
              "studio_project_id",
              "campaign_key",
              "scene",
              "continuity",
              "post_production",
              "provider_strategy",
              "image_to_video")
          .forEach(
              field -> {
                if (requested.has(field)) {
                  merged.set(field, requested.get(field));
                }
              });
      return objectMapper.writeValueAsString(merged);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao combinar metadados solicitados e concluídos do job de vídeo.", ex);
      return completionMetadataJson;
    }
  }

  /** Enfileira a finalização premium após a montagem cinematográfica concluir com sucesso. */
  private void enqueuePremiumFinalization(SalesVideoJob job, String requestedJobMetadata) {
    if (job.getStatus() != SalesVideoStatus.VIDEO_READY
        || !"MUSA_VIDEO_MONTAGE".equalsIgnoreCase(job.getProviderName())
        || !StringUtils.hasText(requestedJobMetadata)) {
      return;
    }
    try {
      JsonNode metadata = objectMapper.readTree(requestedJobMetadata);
      JsonNode finalization = metadata.path("premiumFinalization");
      if (!finalization.path("enabled").asBoolean(false)) {
        return;
      }
      String captionText = finalization.path("captionText").asText("").trim();
      if (!StringUtils.hasText(captionText)) {
        return;
      }
      RequestSalesVideoPostProductionRequest request = new RequestSalesVideoPostProductionRequest();
      request.setRequestedBy(job.getRequestedBy());
      request.setVoiceOverScript(finalization.path("voiceOverScript").asText(null));
      request.setCaptionText(captionText);
      requestPostProduction(job.getId(), request);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao interpretar finalização premium da montagem; jobId={}", job.getId(), ex);
      // O contrato da montagem já foi validado antes do processamento; metadata legado não bloqueia
      // o job concluído.
    }
  }

  @Transactional
  public SalesVideoJobDto fail(Long jobId, JobFailureRequest request) {
    SalesVideoJob job = loadJob(jobId);
    SalesVideoStatus previous = job.getStatus();
    SalesVideoStatus newStatus =
        Optional.ofNullable(request.getStatus()).orElse(SalesVideoStatus.VIDEO_FAILED);
    if (!StringUtils.hasText(job.getMetadataJson())) {
      job.setMetadataJson(jobCostMetadataService.enrichMetadataJson(job, null, null));
    }
    job.setStatus(newStatus);
    job.setFailureCode(request.getFailureCode());
    job.setFailureDetail(request.getFailureDetail());
    job.setFinishedAt(Instant.now());
    jobRepository.save(job);
    syncFailedExperimentVideoAsset(job, request);
    maybeUpdateProfileStatus(job, newStatus);
    registerEvent(
        job,
        SalesVideoJobEventType.FAILED,
        previous,
        newStatus,
        request.getMessage(),
        request.getFailureDetail());
    return toDto(job);
  }

  @Transactional
  public SalesVideoJobDto expire(Long jobId, JobExpirationRequest request) {
    SalesVideoJob job = loadJob(jobId);
    SalesVideoStatus previous = job.getStatus();
    if (!StringUtils.hasText(job.getMetadataJson())) {
      job.setMetadataJson(jobCostMetadataService.enrichMetadataJson(job, null, null));
    }
    job.setStatus(SalesVideoStatus.VIDEO_FAILED);
    job.setFinishedAt(Instant.now());
    jobRepository.save(job);
    maybeUpdateProfileStatus(job, SalesVideoStatus.VIDEO_FAILED);
    registerEvent(
        job,
        SalesVideoJobEventType.EXPIRED,
        previous,
        SalesVideoStatus.VIDEO_FAILED,
        request.getMessage(),
        request.getDetailsJson());
    return toDto(job);
  }

  /** Cria novo job de reprocessamento preservando o contrato operacional do job original. */
  @Transactional
  public SalesVideoJobDto retry(Long jobId, RetrySalesVideoJobRequest request) {
    SalesVideoJob job = loadJob(jobId);
    reprocessPolicy.ensureRetryAllowed(job, request.getReason());
    SalesVideoScript script =
        job.getScript() == null
            ? null
            : scriptRepository.findById(job.getScript().getId()).orElse(null);
    String requestedBy = TenantContextHolder.resolveUserEmail(request.getRequestedBy());
    SalesVideoJob newJob =
        createJob(
            job.getProfile(),
            script,
            job.getJobType(),
            job.getProviderFamily(),
            job.getProviderName(),
            requestedBy,
            job.getExecutionMode());
    newJob.setRetryOfJob(job);
    newJob.setRetryAttempt(job.getRetryAttempt() + 1);
    newJob.setRetryReason(request.getReason());
    newJob.setRetryNotes(request.getNotes());
    newJob.setMetadataJson(job.getMetadataJson());
    jobRepository.save(newJob);
    registerEvent(
        job,
        SalesVideoJobEventType.RETRIED,
        job.getStatus(),
        job.getStatus(),
        "Reprocessamento solicitado por " + requestedBy + " (" + request.getReason().name() + ")",
        request.getNotes());
    return toDto(newJob);
  }

  /** Cria um job de pós-produção a partir de um vídeo bruto já aprovado para visualização. */
  @Transactional
  public SalesVideoJobDto requestPostProduction(
      Long sourceJobId, RequestSalesVideoPostProductionRequest request) {
    SalesVideoJob sourceJob = loadJob(sourceJobId);
    if (sourceJob.getStatus() != SalesVideoStatus.VIDEO_READY) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST, "Pós-produção exige um vídeo com status VIDEO_READY.");
    }
    String sourceVideoUrl = resolveSourceVideoUrl(sourceJob, request.getSourceVideoUrl());
    String requestedBy = TenantContextHolder.resolveUserEmail(request.getRequestedBy());
    SalesVideoJob postProductionJob =
        createJob(
            sourceJob.getProfile(),
            sourceJob.getScript(),
            SalesVideoJobType.POST_PRODUCTION,
            SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
            "MUSA_POST_PRODUCTION",
            requestedBy,
            sourceJob.getExecutionMode());
    postProductionJob.setRetryOfJob(sourceJob);
    postProductionJob.setRetryAttempt(sourceJob.getRetryAttempt() + 1);
    postProductionJob.setMetadataJson(
        buildPostProductionMetadata(sourceJob, sourceVideoUrl, request));
    postProductionJob.setAuditSnapshotJson(
        buildPostProductionAuditSnapshot(sourceJob, postProductionJob, requestedBy));
    jobRepository.save(postProductionJob);
    registerEvent(
        sourceJob,
        SalesVideoJobEventType.RETRIED,
        sourceJob.getStatus(),
        sourceJob.getStatus(),
        "Pós-produção solicitada por " + requestedBy,
        "Job de pós-produção #" + postProductionJob.getId());
    return toDto(postProductionJob);
  }

  /** Cria job de montagem a partir de múltiplos vídeos prontos e auditáveis. */
  @Transactional
  public SalesVideoJobDto requestMontage(RequestSalesVideoMontageRequest request) {
    List<Long> sourceJobIds = normalizeSourceJobIds(request.getSourceJobIds());
    List<SalesVideoJob> sourceJobs =
        orderCinematicSources(sourceJobIds.stream().map(this::loadJob).toList());
    sourceJobs.forEach(this::ensureReadySourceForMontage);
    validateSceneBySceneMontage(sourceJobs);
    SalesVideoJob firstSource = sourceJobs.get(0);
    String requestedBy = TenantContextHolder.resolveUserEmail(request.getRequestedBy());
    SalesVideoJob montageJob =
        createJob(
            firstSource.getProfile(),
            firstSource.getScript(),
            SalesVideoJobType.POST_PRODUCTION,
            SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
            "MUSA_VIDEO_MONTAGE",
            requestedBy,
            firstSource.getExecutionMode());
    montageJob.setRetryOfJob(firstSource);
    montageJob.setRetryAttempt(firstSource.getRetryAttempt() + 1);
    montageJob.setMetadataJson(buildMontageMetadata(sourceJobs));
    montageJob.setAuditSnapshotJson(buildMontageAuditSnapshot(sourceJobs, montageJob, requestedBy));
    jobRepository.save(montageJob);
    registerEvent(
        montageJob,
        SalesVideoJobEventType.PROGRESS,
        montageJob.getStatus(),
        montageJob.getStatus(),
        "Montagem solicitada com " + sourceJobs.size() + " clipes",
        montageJob.getMetadataJson());
    return toDto(montageJob);
  }

  /** Ordena planos cinematográficos pelo storyboard sem confiar na ordem de seleção da tela. */
  private List<SalesVideoJob> orderCinematicSources(List<SalesVideoJob> sourceJobs) {
    if (sourceJobs.stream().allMatch(job -> !readSceneMetadata(job).isMissingNode())) {
      return sourceJobs.stream()
          .sorted(
              java.util.Comparator.comparingInt(
                  job -> readSceneMetadata(job).path("scene").path("order").asInt()))
          .toList();
    }
    return sourceJobs;
  }

  /** Vincula o quadro final auditado do plano anterior como imagem inicial do próximo plano. */
  @Transactional(readOnly = true)
  public String enrichContinuityBridge(
      String metadataJson, Long continuitySourceJobId, Long expectedProfileId) {
    if (continuitySourceJobId == null) {
      return metadataJson;
    }
    SalesVideoJob sourceJob = loadJob(continuitySourceJobId);
    if (sourceJob.getStatus() != SalesVideoStatus.VIDEO_READY
        || sourceJob.getProfile() == null
        || !sourceJob.getProfile().getId().equals(expectedProfileId)
        || sourceJob.getPosterAsset() == null
        || !StringUtils.hasText(sourceJob.getPosterAsset().getUrl())) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "O plano anterior precisa estar pronto e possuir quadro final auditado para continuidade.");
    }
    try {
      ObjectNode metadata =
          StringUtils.hasText(metadataJson)
              ? (ObjectNode) objectMapper.readTree(metadataJson)
              : objectMapper.createObjectNode();
      ObjectNode imageToVideo = metadata.withObject("/image_to_video");
      imageToVideo.put("enabled", true);
      imageToVideo.put("source_image_provider", "PREVIOUS_SCENE_FINAL_FRAME");
      imageToVideo.put("source_image_asset_id", sourceJob.getPosterAsset().getId());
      imageToVideo.put("source_image_url", sourceJob.getPosterAsset().getUrl());
      ObjectNode bridge = metadata.withObject("/continuity_bridge");
      bridge.put("source_job_id", sourceJob.getId());
      bridge.put("source_poster_asset_id", sourceJob.getPosterAsset().getId());
      bridge.put("strategy", "LAST_FRAME_TO_FIRST_FRAME");
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException | ClassCastException ex) {
      log.error(
          "Falha ao montar ponte de continuidade; sourceJobId={} profileId={}",
          continuitySourceJobId,
          expectedProfileId,
          ex);
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST, "Metadata de continuidade inválida.");
    }
  }

  /** Converte job para DTO incluindo custo real ou estimado para a tela. */
  private SalesVideoJobDto toDto(SalesVideoJob job) {
    return jobCostMetadataService.enrichDto(SalesVideoMapper.toDto(job), job);
  }

  private SalesVideoScript maybePersistScriptResult(
      SalesVideoJob job, GeneratedScriptResultPayload payload) {
    if (job.getJobType() != SalesVideoJobType.SCRIPT || payload == null) {
      return null;
    }
    if (!StringUtils.hasText(payload.getScriptText()) || job.getProfile() == null) {
      return null;
    }
    int nextVersion =
        scriptRepository
            .findFirstByProfileIdOrderByVersionDesc(job.getProfile().getId())
            .map(SalesVideoScript::getVersion)
            .map(v -> v + 1)
            .orElse(1);
    String createdBy =
        StringUtils.hasText(job.getRequestedBy()) ? job.getRequestedBy() : "system@marketinghub.io";
    SalesVideoScript script =
        SalesVideoScript.builder()
            .profile(job.getProfile())
            .version(nextVersion)
            .createdBy(createdBy)
            .scriptText(payload.getScriptText())
            .hookText(payload.getHookText())
            .ctaText(payload.getCtaText())
            .captionText(payload.getCaptionText())
            .storyboardJson(payload.getStoryboardJson())
            .source(SalesVideoScriptSource.OPENAI)
            .model(payload.getModel())
            .prompt(payload.getPrompt())
            .status(SalesVideoScriptStatus.READY_FOR_REVIEW)
            .build();
    SalesVideoScript saved = scriptRepository.save(script);
    if (job.getProfile().getScripts() != null) {
      job.getProfile().getScripts().add(saved);
    }
    return saved;
  }

  /** Resolve a URL fonte da pós-produção a partir do payload, streaming ou asset persistido. */
  private String resolveSourceVideoUrl(SalesVideoJob sourceJob, String requestedSourceVideoUrl) {
    if (StringUtils.hasText(requestedSourceVideoUrl)) {
      return requestedSourceVideoUrl.trim();
    }
    if (StringUtils.hasText(sourceJob.getStreamPlaybackUrl())) {
      return sourceJob.getStreamPlaybackUrl().trim();
    }
    if (sourceJob.getAsset() != null && StringUtils.hasText(sourceJob.getAsset().getUrl())) {
      return sourceJob.getAsset().getUrl().trim();
    }
    throw VideoModuleException.badRequest(
        VideoModuleErrorCode.BAD_REQUEST,
        "Informe uma URL fonte ou selecione um vídeo com asset disponível.");
  }

  /** Monta o metadata operacional consumido pelo provider local de pós-produção. */
  private String buildPostProductionMetadata(
      SalesVideoJob sourceJob,
      String sourceVideoUrl,
      RequestSalesVideoPostProductionRequest request) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("sourceJobId", sourceJob.getId());
    metadata.put("sourceVideoUrl", sourceVideoUrl);
    metadata.put("voiceOverScript", request.getVoiceOverScript());
    metadata.put("captionText", request.getCaptionText());
    metadata.put(
        "sourceAssetId", sourceJob.getAsset() != null ? sourceJob.getAsset().getId() : null);
    metadata.put("sourceProviderName", sourceJob.getProviderName());
    metadata.put(
        "commercialIntent",
        "Finalizar video bruto com audio, legenda e trilha para experimento de venda.");
    return writeJson(metadata, "Falha ao serializar metadata de pós-produção.");
  }

  /** Monta snapshot de auditoria para rastrear a origem do vídeo finalizado. */
  private String buildPostProductionAuditSnapshot(
      SalesVideoJob sourceJob, SalesVideoJob postProductionJob, String requestedBy) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("capturedAt", Instant.now().toString());
    snapshot.put("requestedBy", requestedBy);
    snapshot.put("sourceJobId", sourceJob.getId());
    snapshot.put("sourceProviderName", sourceJob.getProviderName());
    snapshot.put(
        "sourceAssetId", sourceJob.getAsset() != null ? sourceJob.getAsset().getId() : null);
    snapshot.put(
        "profileId", sourceJob.getProfile() != null ? sourceJob.getProfile().getId() : null);
    snapshot.put("scriptId", sourceJob.getScript() != null ? sourceJob.getScript().getId() : null);
    snapshot.put("providerFamily", postProductionJob.getProviderFamily());
    snapshot.put("providerName", postProductionJob.getProviderName());
    snapshot.put("executionMode", postProductionJob.getExecutionMode());
    snapshot.put("postProductionMetadataJson", postProductionJob.getMetadataJson());
    return writeJson(snapshot, "Falha ao serializar snapshot de auditoria da pós-produção.");
  }

  /** Normaliza e valida a lista de jobs fonte para montagem. */
  private List<Long> normalizeSourceJobIds(List<Long> sourceJobIds) {
    if (sourceJobIds == null) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST, "Selecione pelo menos dois vídeos para montagem.");
    }
    Set<Long> uniqueIds =
        new LinkedHashSet<>(sourceJobIds.stream().filter(id -> id != null && id > 0).toList());
    if (uniqueIds.size() < 2) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "Selecione pelo menos dois vídeos prontos para montagem.");
    }
    return List.copyOf(uniqueIds);
  }

  /** Garante que o clipe selecionado possui arquivo disponível para composição. */
  private void ensureReadySourceForMontage(SalesVideoJob sourceJob) {
    if (sourceJob.getStatus() != SalesVideoStatus.VIDEO_READY) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "Montagem exige apenas vídeos com status VIDEO_READY. Job inválido: "
              + sourceJob.getId());
    }
    resolveSourceVideoUrl(sourceJob, null);
  }

  /** Exige planos consecutivos do mesmo projeto quando a montagem pertence ao Estúdio. */
  private void validateSceneBySceneMontage(List<SalesVideoJob> sourceJobs) {
    List<JsonNode> sceneMetadata =
        sourceJobs.stream()
            .map(this::readSceneMetadata)
            .filter(node -> !node.isMissingNode())
            .toList();
    if (sceneMetadata.isEmpty()) {
      return;
    }
    if (sceneMetadata.size() != sourceJobs.size() || sourceJobs.size() > MAX_CINEMATIC_SCENES) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "A montagem cinematográfica exige entre dois e doze planos do mesmo projeto.");
    }
    Set<String> projectIds =
        sceneMetadata.stream()
            .map(node -> node.path("studio_project_id").asText())
            .collect(java.util.stream.Collectors.toSet());
    Set<Integer> orders =
        sceneMetadata.stream()
            .map(node -> node.path("scene").path("order").asInt())
            .collect(java.util.stream.Collectors.toSet());
    Set<Integer> expectedOrders =
        java.util.stream.IntStream.rangeClosed(1, sourceJobs.size())
            .boxed()
            .collect(java.util.stream.Collectors.toSet());
    if (projectIds.size() != 1 || projectIds.contains("") || !orders.equals(expectedOrders)) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "Selecione planos consecutivos do mesmo projeto, começando em DOR e terminando em CTA.");
    }
    Map<Integer, String> rolesByOrder =
        sceneMetadata.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    node -> node.path("scene").path("order").asInt(),
                    node -> node.path("scene").path("role").asText()));
    if (!"DOR".equals(rolesByOrder.get(1)) || !"CTA".equals(rolesByOrder.get(sourceJobs.size()))) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "Selecione planos consecutivos do mesmo projeto, começando em DOR e terminando em CTA.");
    }
  }

  /** Lê metadados de cena sem impedir montagens legadas que não usam o Estúdio. */
  private JsonNode readSceneMetadata(SalesVideoJob sourceJob) {
    if (!StringUtils.hasText(sourceJob.getMetadataJson())) {
      return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }
    try {
      JsonNode metadata = objectMapper.readTree(sourceJob.getMetadataJson());
      return "SCENE_BY_SCENE_MONTAGE".equals(metadata.path("generation_strategy").asText())
          ? metadata
          : com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    } catch (JsonProcessingException ex) {
      return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }
  }

  /** Monta o metadata operacional consumido pelo provider local de montagem. */
  private String buildMontageMetadata(List<SalesVideoJob> sourceJobs) {
    List<Map<String, Object>> sources =
        sourceJobs.stream()
            .map(
                sourceJob -> {
                  Map<String, Object> source = new LinkedHashMap<>();
                  source.put("sourceJobId", sourceJob.getId());
                  source.put("sourceVideoUrl", resolveSourceVideoUrl(sourceJob, null));
                  source.put(
                      "sourceAssetId",
                      sourceJob.getAsset() != null ? sourceJob.getAsset().getId() : null);
                  source.put("sourceProviderName", sourceJob.getProviderName());
                  return source;
                })
            .toList();
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("sourceJobIds", sourceJobs.stream().map(SalesVideoJob::getId).toList());
    metadata.put("sourceVideos", sources);
    JsonNode firstSceneMetadata = readSceneMetadata(sourceJobs.get(0));
    if (!firstSceneMetadata.isMissingNode()) {
      JsonNode postProduction = firstSceneMetadata.path("post_production");
      Map<String, Object> premiumFinalization = new LinkedHashMap<>();
      premiumFinalization.put("enabled", true);
      premiumFinalization.put(
          "voiceOverScript",
          sourceJobs.get(0).getScript() != null
              ? sourceJobs.get(0).getScript().getScriptText()
              : null);
      premiumFinalization.put("captionText", postProduction.path("caption_plan").asText(""));
      premiumFinalization.put("soundtrackPlan", postProduction.path("soundtrack_plan").asText(""));
      premiumFinalization.put("ctaText", postProduction.path("cta_text").asText(""));
      metadata.put("premiumFinalization", premiumFinalization);
      metadata.put("transitionStyle", "MOTION_MATCH_CROSSFADE");
      if (sourceJobs.size() >= 6
          && sourceJobs.get(0).getProfile() != null
          && sourceJobs.get(0).getProfile().getTargetDurationSeconds() != null) {
        metadata.put(
            "targetShotSeconds",
            Math.min(
                4.0,
                (double) sourceJobs.get(0).getProfile().getTargetDurationSeconds()
                    / sourceJobs.size()));
      }
      metadata.put(
          "qualityGate",
          Map.of(
              "minimumScenes", sourceJobs.size(),
              "maximumAverageShotSeconds", 4.0,
              "requiresAudio", true,
              "requiresCaptions", true,
              "requiresCta", true));
    }
    metadata.put("maxDurationSeconds", MAX_MONTAGE_DURATION_SECONDS);
    metadata.put(
        "commercialIntent",
        "Montar clipes curtos aprovados em uma sequência única para experimento de venda.");
    return writeJson(metadata, "Falha ao serializar metadata da montagem de vídeo.");
  }

  /** Monta snapshot de auditoria para rastrear os clipes que originaram a montagem. */
  private String buildMontageAuditSnapshot(
      List<SalesVideoJob> sourceJobs, SalesVideoJob montageJob, String requestedBy) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("capturedAt", Instant.now().toString());
    snapshot.put("requestedBy", requestedBy);
    snapshot.put("sourceJobIds", sourceJobs.stream().map(SalesVideoJob::getId).toList());
    snapshot.put(
        "sourceProviderNames", sourceJobs.stream().map(SalesVideoJob::getProviderName).toList());
    snapshot.put(
        "profileId", montageJob.getProfile() != null ? montageJob.getProfile().getId() : null);
    snapshot.put("providerFamily", montageJob.getProviderFamily());
    snapshot.put("providerName", montageJob.getProviderName());
    snapshot.put("executionMode", montageJob.getExecutionMode());
    snapshot.put("montageMetadataJson", montageJob.getMetadataJson());
    return writeJson(snapshot, "Falha ao serializar snapshot de auditoria da montagem de vídeo.");
  }

  /** Serializa objetos simples para JSON de job com erro operacional padronizado. */
  private String writeJson(Map<String, Object> payload, String failureMessage) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw VideoModuleException.internal(VideoModuleErrorCode.INTERNAL_ERROR, failureMessage);
    }
  }

  /** Notifica a porta de sincronizacao de ativos quando um render valido e concluido. */
  private void syncExperimentVideoAsset(
      SalesVideoJob job, JobCompletionRequest request, Integer durationSeconds) {
    if (job.getId() == null
        || !isExperimentVideoAssetSyncJob(job)
        || job.getStatus() != SalesVideoStatus.VIDEO_READY) {
      return;
    }
    completedRenderAssetSync.syncCompletedRender(
        job, request, durationSeconds, resolveResolution(request));
  }

  /** Notifica a porta de sincronizacao quando um render vinculado falha definitivamente. */
  private void syncFailedExperimentVideoAsset(SalesVideoJob job, JobFailureRequest request) {
    if (job.getId() == null
        || !isExperimentVideoAssetSyncJob(job)
        || job.getStatus() != SalesVideoStatus.VIDEO_FAILED) {
      return;
    }
    completedRenderAssetSync.syncFailedRender(job, request);
  }

  /** Identifica jobs que devem atualizar o ativo comercial do experimento. */
  private boolean isExperimentVideoAssetSyncJob(SalesVideoJob job) {
    return job.getJobType() == SalesVideoJobType.RENDER
        || job.getJobType() == SalesVideoJobType.POST_PRODUCTION;
  }

  /** Converte uma conclusao bloqueada em payload de falha para sincronizacao externa. */
  private JobFailureRequest completionFailureRequest(JobCompletionRequest request) {
    JobFailureRequest failureRequest = new JobFailureRequest();
    failureRequest.setStatus(SalesVideoStatus.VIDEO_FAILED);
    failureRequest.setFailureCode(SHORT_DURATION_FAILURE_CODE);
    failureRequest.setFailureDetail(request.getDetailsJson());
    failureRequest.setMessage(request.getMessage());
    return failureRequest;
  }

  /** Extrai a duração auditada do metadata do worker quando disponível. */
  private Integer resolveDurationSeconds(SalesVideoJob job, JobCompletionRequest request) {
    Integer metadataDuration = readIntegerField(request.getMetadataJson(), "duration_seconds");
    if (metadataDuration != null) {
      return metadataDuration;
    }
    return job.getProfile() != null ? job.getProfile().getTargetDurationSeconds() : null;
  }

  /** Valida se o render atingiu a duração mínima da cena isolada ou do perfil comercial. */
  private DurationValidation validateRenderDuration(SalesVideoJob job, Integer durationSeconds) {
    if (job.getJobType() != SalesVideoJobType.RENDER
        && job.getJobType() != SalesVideoJobType.RETRY) {
      return DurationValidation.validResult();
    }
    Integer targetDuration = resolveRenderTargetDuration(job);
    if (targetDuration == null
        || targetDuration <= 0
        || durationSeconds == null
        || durationSeconds <= 0) {
      return DurationValidation.validResult();
    }
    int minimumAcceptedDuration = minimumAcceptedDuration(targetDuration);
    if (durationSeconds >= minimumAcceptedDuration) {
      return DurationValidation.validResult();
    }
    return DurationValidation.invalid(
        "Render rejeitado: duração auditada de "
            + durationSeconds
            + "s ficou abaixo do mínimo comercial de "
            + minimumAcceptedDuration
            + "s para perfil alvo de "
            + targetDuration
            + "s. Gere uma sequência de clipes ou novo render antes de publicar.");
  }

  /** Usa o contrato da cena isolada antes da duração do vídeo final definida no perfil. */
  private Integer resolveRenderTargetDuration(SalesVideoJob job) {
    if (StringUtils.hasText(job.getMetadataJson())) {
      try {
        JsonNode metadata = objectMapper.readTree(job.getMetadataJson());
        if ("SCENE_BY_SCENE_MONTAGE"
                .equalsIgnoreCase(metadata.path("generation_strategy").asText(""))
            && metadata.path("scene").isObject()) {
          int duration =
              metadata
                  .path("provider_strategy")
                  .path("expected_clip_duration_seconds")
                  .asInt(metadata.path("scene").path("duration_seconds").asInt(0));
          if (duration > 0) {
            return duration;
          }
        }
      } catch (JsonProcessingException ignored) {
        // O metadata inválido será tratado pelos validadores próprios do contrato do job.
      }
    }
    return job.getProfile() != null ? job.getProfile().getTargetDurationSeconds() : null;
  }

  /** Calcula a menor duracao aceita com tolerancia para arredondamentos do provider. */
  private int minimumAcceptedDuration(int targetDuration) {
    int toleranceSeconds = Math.max(2, (int) Math.ceil(targetDuration * 0.10));
    return Math.max(1, targetDuration - toleranceSeconds);
  }

  /** Define a mensagem do evento de conclusão respeitando bloqueios comerciais. */
  private String completionMessage(
      JobCompletionRequest request, DurationValidation durationValidation) {
    if (!durationValidation.valid()) {
      return durationValidation.message();
    }
    return request.getMessage();
  }

  /** Define os detalhes do evento de conclusão respeitando bloqueios comerciais. */
  private String completionDetails(
      JobCompletionRequest request, DurationValidation durationValidation) {
    if (!durationValidation.valid()) {
      return durationValidation.message();
    }
    return request.getDetailsJson();
  }

  /** Extrai a resolução auditada do metadata do worker quando disponível. */
  private String resolveResolution(JobCompletionRequest request) {
    return readStringField(request.getMetadataJson(), "resolution");
  }

  /** Normaliza a URL HLS/DASH publicavel recebida do pipeline de midia. */
  private String normalizeStreamPlaybackUrl(String streamPlaybackUrl) {
    return StringUtils.hasText(streamPlaybackUrl) ? streamPlaybackUrl.trim() : null;
  }

  /** Lê um campo numérico simples de um payload JSON de metadata. */
  private Integer readIntegerField(String json, String fieldName) {
    JsonNode value = readJsonField(json, fieldName);
    if (value == null || !value.canConvertToInt()) {
      return null;
    }
    return value.asInt();
  }

  /** Lê um campo textual simples de um payload JSON de metadata. */
  private String readStringField(String json, String fieldName) {
    JsonNode value = readJsonField(json, fieldName);
    if (value == null || value.isNull()) {
      return null;
    }
    return value.isTextual() ? value.asText() : value.toString();
  }

  /** Lê um campo simples de um payload JSON de metadata usando parser estruturado. */
  private JsonNode readJsonField(String json, String fieldName) {
    if (!StringUtils.hasText(json) || !StringUtils.hasText(fieldName)) {
      return null;
    }
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode value = root.path(fieldName);
      return value.isMissingNode() ? null : value;
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      return null;
    }
  }

  private void attachAsset(java.util.function.Consumer<Asset> setter, Long assetId) {
    if (assetId == null) {
      setter.accept(null);
      return;
    }
    Asset asset =
        assetRepository
            .findById(assetId)
            .orElseThrow(
                () ->
                    VideoModuleException.badRequest(
                        VideoModuleErrorCode.ASSET_NOT_FOUND, "Asset não encontrado: " + assetId));
    setter.accept(asset);
  }

  private SalesVideoJob loadJob(Long jobId) {
    SalesVideoJob job =
        jobRepository
            .findById(jobId)
            .orElseThrow(
                () ->
                    VideoModuleException.notFound(
                        VideoModuleErrorCode.JOB_NOT_FOUND, "Job não encontrado: " + jobId));
    TenantContextHolder.assertTenant(job.getTenantId());
    return job;
  }

  private void ensureJobExists(Long jobId) {
    if (!jobRepository.existsById(jobId)) {
      throw VideoModuleException.notFound(
          VideoModuleErrorCode.JOB_NOT_FOUND, "Job não encontrado: " + jobId);
    }
  }

  private void ensureProfileAccessible(Long profileId) {
    SalesVideoProfile profile =
        profileRepository
            .findById(profileId)
            .orElseThrow(
                () ->
                    VideoModuleException.notFound(
                        VideoModuleErrorCode.PROFILE_NOT_FOUND,
                        "Perfil de vídeo não encontrado: " + profileId));
    TenantContextHolder.assertTenant(profile.getTenantId());
  }

  private SalesVideoStatus initialStatus(SalesVideoJobType jobType) {
    return switch (jobType) {
      case SCRIPT -> SalesVideoStatus.SCRIPT_PENDING;
      case STORYBOARD -> SalesVideoStatus.STORYBOARD_PENDING;
      case RENDER, POST_PRODUCTION, RETRY -> SalesVideoStatus.VIDEO_REQUESTED;
      case PUBLISH -> SalesVideoStatus.PUBLISHED;
    };
  }

  private SalesVideoStatus defaultCompletionStatus(SalesVideoJobType jobType) {
    return switch (jobType) {
      case SCRIPT -> SalesVideoStatus.SCRIPT_READY;
      case STORYBOARD -> SalesVideoStatus.STORYBOARD_READY;
      case RENDER, POST_PRODUCTION, RETRY -> SalesVideoStatus.VIDEO_READY;
      case PUBLISH -> SalesVideoStatus.PUBLISHED;
    };
  }

  private void registerEvent(
      SalesVideoJob job,
      SalesVideoJobEventType type,
      SalesVideoStatus oldStatus,
      SalesVideoStatus newStatus,
      String message,
      String detailsJson) {
    SalesVideoJobEvent event =
        SalesVideoJobEvent.builder()
            .job(job)
            .eventType(type)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .message(message)
            .detailsJson(detailsJson)
            .build();
    eventRepository.save(event);
    job.getEvents().add(event);
  }

  private void maybeUpdateProfileStatus(SalesVideoJob job, SalesVideoStatus status) {
    if (status == null || job.getProfile() == null) {
      return;
    }
    EnumSet<SalesVideoStatus> syncable =
        EnumSet.of(
            SalesVideoStatus.SCRIPT_PENDING,
            SalesVideoStatus.SCRIPT_READY,
            SalesVideoStatus.STORYBOARD_PENDING,
            SalesVideoStatus.STORYBOARD_READY,
            SalesVideoStatus.VIDEO_REQUESTED,
            SalesVideoStatus.VIDEO_PROCESSING,
            SalesVideoStatus.VIDEO_READY,
            SalesVideoStatus.VIDEO_FAILED,
            SalesVideoStatus.PUBLISHED);
    if (syncable.contains(status)) {
      job.getProfile().setStatus(status);
      profileRepository.save(job.getProfile());
    }
  }

  /** Resultado interno da validacao de duracao do render. */
  private record DurationValidation(boolean valid, String message) {
    /** Cria resultado valido sem mensagem de bloqueio. */
    private static DurationValidation validResult() {
      return new DurationValidation(true, null);
    }

    /** Cria resultado invalido com a causa do bloqueio. */
    private static DurationValidation invalid(String message) {
      return new DurationValidation(false, message);
    }
  }
}
