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
import java.time.temporal.ChronoUnit;
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
  private static final long JOB_LEASE_MINUTES = 10;

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
  private final ApolloBudgetMonitorService apolloBudgetMonitorService;

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
      ApolloBudgetMonitorService apolloBudgetMonitorService,
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
    this.apolloBudgetMonitorService = apolloBudgetMonitorService;
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
    syncStudioCostLedger(saved, null, false);
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

  /** Reserva atomicamente um job novo ou órfão e inicia sua lease operacional. */
  @Transactional
  public SalesVideoJobDto claimJob(Long jobId, JobClaimRequest request) {
    SalesVideoJob candidate = loadJob(jobId);
    SalesVideoStatus previous = candidate.getStatus();
    Instant claimedAt = Instant.now();
    int claimed =
        jobRepository.claimIfAvailable(
            jobId,
            SalesVideoStatus.VIDEO_REQUESTED,
            SalesVideoStatus.VIDEO_PROCESSING,
            claimedAt,
            claimedAt.minus(JOB_LEASE_MINUTES, ChronoUnit.MINUTES));
    if (claimed != 1) {
      throw VideoModuleException.conflict(
          VideoModuleErrorCode.JOB_CLAIM_CONFLICT,
          "Job já reservado, concluído ou indisponível para claim: " + jobId);
    }
    SalesVideoJob job = loadJob(jobId);
    registerEvent(
        job,
        SalesVideoJobEventType.CLAIMED,
        previous,
        SalesVideoStatus.VIDEO_PROCESSING,
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
    Optional<VideoProject> project =
        projectId != null
            ? videoProjectRepository.findById(projectId)
            : job.getProfile() == null || job.getProfile().getId() == null
                ? Optional.empty()
                : videoProjectRepository.findFirstBySalesVideoProfileIdOrderByUpdatedAtDesc(
                    job.getProfile().getId());
    Long productId = project.map(VideoProject::getProductId).orElseGet(() -> profileProductId(job));
    if (productId == null) {
      log.error("Job do Estúdio sem produto para atribuição financeira; jobId={}", job.getId());
      return;
    }
    studioCostLedgerService.recordVideo(
        job.getId(),
        readVideoProductionCycleId(job.getMetadataJson()),
        productId,
        project.map(VideoProject::getCommercialPlanId).orElse(null),
        project.map(VideoProject::getExperimentId).orElse(null),
        studioAssetType(job),
        job.getProviderName(),
        job.getProviderName(),
        job.getStatus().name(),
        costUsd,
        providerReported,
        job.getStartedAt() != null ? job.getStartedAt() : job.getRequestedAt(),
        job.getFinishedAt());
  }

  /** Extrai o ciclo financeiro explícito para segregar cada novo custo. */
  private Long readVideoProductionCycleId(String metadataJson) {
    if (!StringUtils.hasText(metadataJson)) return null;
    try {
      JsonNode value = objectMapper.readTree(metadataJson).path("videoProductionCycleId");
      return value.canConvertToLong() && value.asLong() > 0 ? value.asLong() : null;
    } catch (JsonProcessingException ex) {
      log.error("Falha ao ler ciclo financeiro para ledger; metadata={}", metadataJson, ex);
      return null;
    }
  }

  /** Recupera o produto diretamente do perfil quando o projeto ainda não foi vinculado. */
  private Long profileProductId(SalesVideoJob job) {
    return job.getProfile() != null && job.getProfile().getProduct() != null
        ? job.getProfile().getProduct().getId()
        : null;
  }

  /** Distingue voz/finalização de montagem e demais tentativas de vídeo no relatório financeiro. */
  private String studioAssetType(SalesVideoJob job) {
    return job.getJobType() == SalesVideoJobType.POST_PRODUCTION
            && (job.getProviderName() == null || !job.getProviderName().contains("MONTAGE"))
        ? "AUDIO"
        : "VIDEO";
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

  /** Renova a lease do worker sem alterar o estado funcional do job. */
  @Transactional
  public SalesVideoJobDto heartbeat(Long jobId, JobHeartbeatRequest request) {
    SalesVideoJob job = loadJob(jobId);
    if (isTerminal(job.getStatus())) {
      return toDto(job);
    }
    job.setUpdatedAt(Instant.now());
    jobRepository.save(job);
    registerEvent(
        job,
        SalesVideoJobEventType.HEARTBEAT,
        job.getStatus(),
        job.getStatus(),
        request.getMessage(),
        request.getDetailsJson());
    return toDto(job);
  }

  /** Registra progresso sem permitir que callback atrasado reabra um job terminal. */
  @Transactional
  public SalesVideoJobDto progress(Long jobId, JobProgressRequest request) {
    SalesVideoJob job = loadJob(jobId);
    if (isTerminal(job.getStatus())) {
      log.warn(
          "Progresso atrasado ignorado para job terminal; jobId={} status={}",
          jobId,
          job.getStatus());
      return toDto(job);
    }
    if (request.getProgressPercent() != null) {
      job.setProgressPercent(Math.max(0, Math.min(100, request.getProgressPercent())));
    }
    SalesVideoStatus oldStatus = job.getStatus();
    if (request.getStatus() != null && request.getStatus() != job.getStatus()) {
      job.setStatus(request.getStatus());
      maybeUpdateProfileStatus(job, request.getStatus());
    }
    jobRepository.save(job);
    syncProviderTaskConsumption(job, request.getDetailsJson());
    registerEvent(
        job,
        SalesVideoJobEventType.PROGRESS,
        oldStatus,
        job.getStatus(),
        request.getMessage(),
        request.getDetailsJson());
    return toDto(job);
  }

  /** Concilia imediatamente uma task/cena aceita sem depender do desfecho do job final. */
  private void syncProviderTaskConsumption(SalesVideoJob job, String detailsJson) {
    if (studioCostLedgerService == null || !StringUtils.hasText(detailsJson)) return;
    try {
      JsonNode details = objectMapper.readTree(detailsJson);
      String eventType = details.path("eventType").asText();
      String taskId = details.path("providerTaskId").asText("").trim();
      if (!StringUtils.hasText(taskId)) return;
      if ("PROVIDER_TASK_SETTLED".equals(eventType)) {
        studioCostLedgerService.settleProviderTask(
            job.getId(),
            details.path("provider").asText("UNKNOWN"),
            taskId,
            details.path("billedCredits").asInt(),
            details.path("billedCostUsd").decimalValue(),
            details.path("settlementStatus").asText(),
            details.path("settlementBasis").asText("UNKNOWN"),
            details.path("billingEvidence").asText(),
            Instant.now(),
            job.getStatus().name());
        monitorApolloBudget(job, taskId);
        return;
      }
      if (!"PROVIDER_TASK_ACCEPTED".equals(eventType)) return;
      studioCostLedgerService.recordProviderTask(
          job.getId(),
          readVideoProductionCycleId(job.getMetadataJson()),
          details.path("provider").asText("UNKNOWN"),
          taskId,
          details.path("model").asText(job.getProviderName()),
          details.path("sceneNumber").asInt(),
          details.path("plannedSceneCount").asInt(),
          details.path("durationSeconds").asInt(),
          details.path("estimatedCredits").asInt(),
          details.path("estimatedCostUsd").decimalValue(),
          Instant.now(),
          job.getStatus().name());
      monitorApolloBudget(job, taskId);
    } catch (JsonProcessingException | RuntimeException ex) {
      log.error(
          "Falha ao conciliar task de provedor; jobId={} details={}", job.getId(), detailsJson, ex);
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST, "Evento financeiro de task inválido.");
    }
  }

  /** Atualiza o alerta financeiro canônico imediatamente após o callback da task. */
  private void monitorApolloBudget(SalesVideoJob job, String providerTaskId) {
    if (apolloBudgetMonitorService == null) return;
    apolloBudgetMonitorService.reconcile(
        readVideoProductionCycleId(job.getMetadataJson()), job.getId(), providerTaskId);
  }

  /** Finaliza o job, aplica gates de duração e encadeia a pós-produção cinematográfica. */
  @Transactional
  public SalesVideoJobDto complete(Long jobId, JobCompletionRequest request) {
    SalesVideoJob job = loadJob(jobId);
    if (isSuccessfulTerminal(job.getStatus())) {
      if (sameCompletion(job, request)) {
        return toDto(job);
      }
      throw VideoModuleException.conflict(
          VideoModuleErrorCode.JOB_CLAIM_CONFLICT, "Job já concluído por outra execução: " + jobId);
    }
    if (job.getStatus() == SalesVideoStatus.VIDEO_FAILED) {
      throw VideoModuleException.conflict(
          VideoModuleErrorCode.JOB_CLAIM_CONFLICT,
          "Job já finalizado com falha; solicite novo processamento: " + jobId);
    }
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
    } else if (finalStatus != SalesVideoStatus.VIDEO_FAILED) {
      job.setFailureCode(null);
      job.setFailureDetail(null);
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
    settleApolloReservation(job);
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

  /** Reconhece recusas financeiras sem depender do código textual de um único provider. */
  private boolean isInsufficientCredits(String detail) {
    if (!StringUtils.hasText(detail)) return false;
    String normalized = detail.toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("insufficient credits")
        || normalized.contains("not enough credits")
        || normalized.contains("saldo insuficiente");
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
              "image_to_video",
              "technicalQualityGate",
              "referenceGovernance",
              "premiumFinalization",
              "researchIntelligence")
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

  /** Enfileira a finalização premium após a fonte visual governada concluir com sucesso. */
  private void enqueuePremiumFinalization(SalesVideoJob job, String requestedJobMetadata) {
    if (job.getStatus() != SalesVideoStatus.VIDEO_READY
        || !("MUSA_VIDEO_MONTAGE".equalsIgnoreCase(job.getProviderName())
            || "RUNWAY_PRODUCT_UGC".equalsIgnoreCase(job.getProviderName()))
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

  /** Registra a falha terminal e preserva a tentativa no ledger financeiro do Estúdio. */
  @Transactional
  public SalesVideoJobDto fail(Long jobId, JobFailureRequest request) {
    SalesVideoJob job = loadJob(jobId);
    if (isSuccessfulTerminal(job.getStatus())) {
      log.warn(
          "Falha atrasada ignorada após conclusão do vídeo; jobId={} failureCode={}",
          jobId,
          request.getFailureCode());
      return toDto(job);
    }
    if (job.getStatus() == SalesVideoStatus.VIDEO_FAILED) {
      return toDto(job);
    }
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
    if (apolloBudgetMonitorService != null && isInsufficientCredits(request.getFailureDetail())) {
      apolloBudgetMonitorService.blockForInsufficientCredits(
          readVideoProductionCycleId(job.getMetadataJson()),
          job.getId(),
          request.getFailureDetail());
    }
    syncStudioCostLedger(job, null, false);
    settleApolloReservation(job);
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

  /** Expira o job e preserva a tentativa no ledger financeiro do Estúdio. */
  @Transactional
  public SalesVideoJobDto expire(Long jobId, JobExpirationRequest request) {
    SalesVideoJob job = loadJob(jobId);
    if (isTerminal(job.getStatus())) {
      return toDto(job);
    }
    SalesVideoStatus previous = job.getStatus();
    if (!StringUtils.hasText(job.getMetadataJson())) {
      job.setMetadataJson(jobCostMetadataService.enrichMetadataJson(job, null, null));
    }
    job.setStatus(SalesVideoStatus.VIDEO_FAILED);
    job.setFinishedAt(Instant.now());
    jobRepository.save(job);
    syncStudioCostLedger(job, null, false);
    settleApolloReservation(job);
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

  /** Identifica estados cujo resultado não pode ser regredido por callbacks atrasados. */
  private boolean isTerminal(SalesVideoStatus status) {
    return status == SalesVideoStatus.VIDEO_FAILED || isSuccessfulTerminal(status);
  }

  /** Identifica sucesso funcional que não pode ser substituído por uma falha atrasada. */
  private boolean isSuccessfulTerminal(SalesVideoStatus status) {
    return status == SalesVideoStatus.SCRIPT_READY
        || status == SalesVideoStatus.STORYBOARD_READY
        || status == SalesVideoStatus.VIDEO_READY
        || status == SalesVideoStatus.PUBLISHED
        || status == SalesVideoStatus.ARCHIVED;
  }

  /** Reconhece repetição idempotente do mesmo callback de conclusão. */
  private boolean sameCompletion(SalesVideoJob job, JobCompletionRequest request) {
    Long existingAssetId = job.getAsset() == null ? null : job.getAsset().getId();
    if (existingAssetId == null && !StringUtils.hasText(job.getProviderJobId())) {
      return false;
    }
    return java.util.Objects.equals(existingAssetId, request.getAssetId())
        && java.util.Objects.equals(job.getProviderJobId(), request.getProviderJobId());
  }

  /** Libera a parcela não consumida e preserva custo real quando um job termina. */
  private void settleApolloReservation(SalesVideoJob job) {
    if (apolloBudgetMonitorService == null) return;
    apolloBudgetMonitorService.settleReservation(readVideoProductionCycleId(job.getMetadataJson()));
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
    if (!isReusableRenderWithAsset(sourceJob)) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "Pós-produção exige vídeo pronto ou render curto com arquivo preservado.");
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
    syncStudioCostLedger(postProductionJob, null, false);
    registerEvent(
        sourceJob,
        SalesVideoJobEventType.RETRIED,
        sourceJob.getStatus(),
        sourceJob.getStatus(),
        "Pós-produção solicitada por " + requestedBy,
        "Job de pós-produção #" + postProductionJob.getId());
    return toDto(postProductionJob);
  }

  /** Permite reaproveitar montagem tecnicamente produzida que falhou apenas no gate de duração. */
  private boolean isReusableRenderWithAsset(SalesVideoJob sourceJob) {
    if (sourceJob.getStatus() == SalesVideoStatus.VIDEO_READY) {
      return true;
    }
    return sourceJob.getAsset() != null
        && sourceJob.getStatus() == SalesVideoStatus.VIDEO_FAILED
        && SHORT_DURATION_FAILURE_CODE.equals(sourceJob.getFailureCode());
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
    syncStudioCostLedger(montageJob, null, false);
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
    SalesVideoJobDto dto = jobCostMetadataService.enrichDto(SalesVideoMapper.toDto(job), job);
    CommercialReadiness readiness = assessCommercialReadiness(job);
    dto.setCommercialReadinessStatus(readiness.status());
    dto.setCommercialReadinessBlockers(readiness.blockers());
    return dto;
  }

  /** Avalia no backend se o job representa uma peça comercial final e auditável. */
  private CommercialReadiness assessCommercialReadiness(SalesVideoJob job) {
    List<String> blockers = new java.util.ArrayList<>();
    if (job.getStatus() != SalesVideoStatus.VIDEO_READY) {
      blockers.add("A renderização final ainda não foi concluída.");
    }
    if (!"MUSA_POST_PRODUCTION".equalsIgnoreCase(job.getProviderName())) {
      blockers.add("O ativo ainda é um clipe bruto ou uma montagem sem pós-produção final.");
    }
    if (!StringUtils.hasText(job.getStreamPlaybackUrl())
        || !job.getStreamPlaybackUrl().contains(".m3u8")) {
      blockers.add("A playlist HLS publicável não foi registrada.");
    }
    JsonNode metadata = readJobMetadata(job);
    if (!metadata.path("audio").path("voice_over").asBoolean(false)
        || !"pt-BR".equalsIgnoreCase(metadata.path("audio").path("language").asText())) {
      blockers.add("A narração em português do Brasil não foi incorporada.");
    }
    if (!metadata.path("captions").path("burned_in").asBoolean(false)
        || !metadata.path("captions").path("vtt_asset").asBoolean(false)
        || job.getVttAsset() == null) {
      blockers.add("As legendas mobile queimadas e o arquivo VTT não estão completos.");
    }
    if (!hasCommercialCta(job, metadata)) {
      blockers.add("O CTA final não possui evidência persistida no roteiro ou na pós-produção.");
    }
    SalesVideoJob sourceJob = job.getRetryOfJob();
    if (sourceJob == null
        || !("MUSA_VIDEO_MONTAGE".equalsIgnoreCase(sourceJob.getProviderName())
            || "RUNWAY_PRODUCT_UGC".equalsIgnoreCase(sourceJob.getProviderName()))) {
      blockers.add("A peça não deriva de uma fonte visual narrativa auditável.");
    }
    if (sourceJob != null && "RUNWAY_PRODUCT_UGC".equalsIgnoreCase(sourceJob.getProviderName())) {
      addProductUgcQualityBlockers(metadata, blockers);
    }
    if (job.getProfile() == null || job.getProfile().getHumanReviewApprovedAt() == null) {
      blockers.add("A revisão humana final ainda não foi aprovada.");
    }
    return new CommercialReadiness(blockers.isEmpty() ? "READY" : "BLOCKED", List.copyOf(blockers));
  }

  /** Lê os metadados persistidos sem transformar JSON inválido em aprovação comercial. */
  private JsonNode readJobMetadata(SalesVideoJob job) {
    if (!StringUtils.hasText(job.getMetadataJson())) {
      return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }
    try {
      return objectMapper.readTree(job.getMetadataJson());
    } catch (JsonProcessingException ex) {
      log.warn("Metadata inválido bloqueou gate comercial; jobId={}", job.getId(), ex);
      return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }
  }

  /** Exige as medições finais que atacam as causas da rejeição anterior do Product UGC. */
  private void addProductUgcQualityBlockers(JsonNode metadata, List<String> blockers) {
    if (!"APPROVED"
        .equalsIgnoreCase(
            metadata.path("apollo_technical_quality").path("stability_status").asText())) {
      blockers.add("Apolo ainda não aprovou a estabilidade da tomada contínua.");
    }
    JsonNode synchronization = metadata.path("caption_narration_sync");
    if (!"APPROVED".equalsIgnoreCase(synchronization.path("status").asText())
        || !"APPROVED".equalsIgnoreCase(synchronization.path("timing_status").asText())) {
      blockers.add("Texto, locução e tempo de exibição ainda não possuem sincronismo aprovado.");
    }
    if (!"APPROVED_FOR_TEST"
        .equalsIgnoreCase(metadata.path("audio").path("review").path("status").asText())) {
      blockers.add("O áudio premium ainda não passou no gate técnico para teste comercial.");
    }
  }

  /** Confirma CTA funcional no roteiro aprovado ou no contrato final persistido. */
  private boolean hasCommercialCta(SalesVideoJob job, JsonNode metadata) {
    return (job.getScript() != null && StringUtils.hasText(job.getScript().getCtaText()))
        || StringUtils.hasText(metadata.path("ctaText").asText())
        || StringUtils.hasText(metadata.path("cta_text").asText());
  }

  /** Representa a decisão derivada do gate comercial e suas causas objetivas. */
  private record CommercialReadiness(String status, List<String> blockers) {}

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
    preserveGovernedLineage(sourceJob, metadata);
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

  /**
   * Preserva os identificadores e o plano narrativo do ciclo para que o vídeo final volte ao
   * experimento que o originou.
   */
  private void preserveGovernedLineage(SalesVideoJob sourceJob, Map<String, Object> target) {
    if (!StringUtils.hasText(sourceJob.getMetadataJson())) {
      return;
    }
    try {
      JsonNode source = objectMapper.readTree(sourceJob.getMetadataJson());
      List.of(
              "videoProductionCycleId",
              "videoProjectId",
              "studio_project_id",
              "productId",
              "experimentId",
              "campaign_key",
              "generation_strategy",
              "targetDurationSeconds",
              "sceneCount",
              "assemblyRequired",
              "runwayRouterConfigId",
              "runwayRouterRequestsJson",
              "cut_plan",
              "post_production",
              "technicalQualityGate",
              "referenceGovernance",
              "premiumFinalization",
              "researchIntelligence",
              "apollo_technical_quality")
          .forEach(
              field -> {
                if (source.has(field)) {
                  target.put(field, source.get(field));
                }
              });
      reconcilePostProductionTechnicalGate(source, target);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao preservar linhagem governada na pós-produção; sourceJobId={}",
          sourceJob.getId(),
          ex);
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "Metadata do vídeo fonte não permite preservar a linhagem do experimento.");
    }
  }

  /** Faz o contrato filho refletir a estabilidade já auditada no arquivo Product UGC fonte. */
  private void reconcilePostProductionTechnicalGate(JsonNode source, Map<String, Object> target) {
    JsonNode audit = source.path("apollo_technical_quality");
    if (!"RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION"
            .equalsIgnoreCase(source.path("generation_strategy").asText())
        || !"APPROVED".equalsIgnoreCase(audit.path("stability_status").asText())
        || !audit.path("method").asText().startsWith("FFMPEG_SCENE_AWARE_")) {
      return;
    }
    ObjectNode gate =
        source.path("technicalQualityGate").isObject()
            ? ((ObjectNode) source.path("technicalQualityGate")).deepCopy()
            : objectMapper.createObjectNode();
    boolean cutsAllowed = audit.path("intentional_scene_cuts_allowed").asBoolean(false);
    gate.put("continuousTakeRequired", !cutsAllowed);
    gate.put("intentionalSceneCutsAllowed", cutsAllowed);
    gate.put("maximumSceneCuts", audit.path("maximum_scene_cuts").asInt(0));
    target.put("technicalQualityGate", gate);
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
    Map<Integer, String> requiredRolesByOrder =
        Map.of(1, "DOR", 2, "RESULTADO", 3, "MECANISMO", 4, "CTA");
    if (!rolesByOrder.equals(requiredRolesByOrder)) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "A montagem comercial exige exatamente DOR, RESULTADO, MECANISMO e CTA, nesta sequência narrativa.");
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
        if ("RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION"
                .equalsIgnoreCase(metadata.path("generation_strategy").asText(""))
            && metadata.path("targetDurationSeconds").asInt(0) > 0) {
          return metadata.path("targetDurationSeconds").asInt();
        }
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
      } catch (JsonProcessingException ex) {
        log.warn("Metadata inválido ao resolver duração do render; jobId={}", job.getId(), ex);
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
