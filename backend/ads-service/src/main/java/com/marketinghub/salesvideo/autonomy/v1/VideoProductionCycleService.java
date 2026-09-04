package com.marketinghub.salesvideo.autonomy.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskByAgentRequest;
import com.marketinghub.agenttask.DecideAgentGateRequest;
import com.marketinghub.financialagent.service.FinancialAgentService;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoExecutionMode;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.VideoCreditReservation;
import com.marketinghub.salesvideo.VideoProductionCycle;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.VideoProviderPreflight;
import com.marketinghub.salesvideo.dto.RequestSalesVideoPostProductionRequest;
import com.marketinghub.salesvideo.dto.RequestVideoRenderRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.mapper.VideoProjectResearchIntelligenceMapper;
import com.marketinghub.salesvideo.service.SalesVideoService;
import com.marketinghub.salesvideo.service.providerpreflight.VideoProviderFinancialPreflightData;
import com.marketinghub.salesvideo.service.providerpreflight.VideoProviderFinancialPreflightService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: coordenar o gate de Plutus e a entrega de ciclos aprovados a Apolo. */
@Service
public class VideoProductionCycleService {
  private static final Logger log = LoggerFactory.getLogger(VideoProductionCycleService.class);
  private static final String PLUTUS_KEY = "financial-agent";
  private static final String APOLLO_KEY = "videomaker";
  private static final String RUNWAY_ROUTER = "RUNWAY_ROUTER";
  private static final String RUNWAY_PRODUCT_UGC = "RUNWAY_PRODUCT_UGC";
  private static final String APOLLO_BLOCKED = "APOLLO_BLOCKED";
  private final VideoProductionCycleRepository repository;
  private final VideoProjectRepository projectRepository;
  private final SalesVideoJobRepository jobRepository;
  private final AgentTaskService taskService;
  private final SalesVideoService salesVideoService;
  private final FinancialAgentService financialAgentService;
  private final StudioCostLedgerService studioCostLedgerService;
  private final VideoProviderFinancialPreflightService providerPreflightService;
  private final ObjectMapper objectMapper;
  private VideoProjectResearchIntelligenceMapper researchIntelligenceMapper;

  /** Configura persistência, caixas de entrada e o executor canônico de vídeo. */
  public VideoProductionCycleService(
      VideoProductionCycleRepository repository,
      VideoProjectRepository projectRepository,
      SalesVideoJobRepository jobRepository,
      AgentTaskService taskService,
      SalesVideoService salesVideoService,
      FinancialAgentService financialAgentService,
      StudioCostLedgerService studioCostLedgerService,
      VideoProviderFinancialPreflightService providerPreflightService,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.projectRepository = projectRepository;
    this.jobRepository = jobRepository;
    this.taskService = taskService;
    this.salesVideoService = salesVideoService;
    this.financialAgentService = financialAgentService;
    this.studioCostLedgerService = studioCostLedgerService;
    this.providerPreflightService = providerPreflightService;
    this.objectMapper = objectMapper;
  }

  /** Conecta a biblioteca comum usada para auditar o contexto entregue a Apolo. */
  @Autowired
  public void setResearchIntelligenceMapper(
      VideoProjectResearchIntelligenceMapper researchIntelligenceMapper) {
    this.researchIntelligenceMapper = researchIntelligenceMapper;
  }

  /** Abre um ciclo bloqueado e solicita primeiro o preflight oficial do agregador. */
  @Transactional
  public VideoProductionCycleContracts.Response create(
      VideoProductionCycleContracts.CreateRequest request) {
    return create(request, "PENDING_PROVIDER_PREFLIGHT");
  }

  /** Abre uma verificação isolada que nunca reserva créditos nem enfileira geração. */
  @Transactional
  public VideoProductionCycleContracts.Response createProviderPreflight(
      VideoProductionCycleContracts.CreateRequest request) {
    return create(request, "PENDING_PROVIDER_PREFLIGHT_ONLY");
  }

  /** Persiste o contexto comum e abre o preflight oficial no modo solicitado. */
  private VideoProductionCycleContracts.Response create(
      VideoProductionCycleContracts.CreateRequest request, String initialStatus) {
    VideoProject project = project(request.videoProjectId());
    validateProviderPlan(project);
    if (project.getSalesVideoProfileId() == null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O projeto precisa de um perfil de vídeo antes do ciclo autônomo.");
    }
    Instant now = Instant.now();
    VideoProductionCycle cycle = new VideoProductionCycle();
    cycle.setVideoProjectId(project.getId());
    cycle.setProductId(project.getProductId());
    cycle.setCommercialPlanId(project.getCommercialPlanId());
    cycle.setExperimentId(project.getExperimentId());
    cycle.setRequestedBy(request.requestedBy().trim());
    cycle.setStatus(initialStatus);
    cycle.setBudgetLimitUsd(request.budgetLimitUsd());
    cycle.setKnownCostUsd(BigDecimal.ZERO);
    cycle.setLearningObjective(request.learningObjective().trim());
    cycle.setSuccessCriterion(request.successCriterion().trim());
    cycle.setCreatedAt(now);
    cycle.setUpdatedAt(now);
    cycle = repository.save(cycle);
    providerPreflightService.open(cycle.getId(), request.productionProfile());
    return response(repository.save(cycle));
  }

  /** Lista os ciclos cujo saldo, quota e payload ainda precisam de dry run no executor. */
  @Transactional(readOnly = true)
  public List<VideoProviderPreflightContracts.PendingResponse> pendingProviderPreflight() {
    return providerPreflightService.pending().stream()
        .map(
            preflight -> {
              VideoProductionCycle cycle = cycle(preflight.getVideoProductionCycleId());
              return providerPreflightPendingResponse(
                  providerPreflightService.pendingResponse(
                      preflight, cycle, project(cycle.getVideoProjectId())));
            })
        .toList();
  }

  /** Recebe o preflight e respeita o modo isolado antes de qualquer reserva ou tarefa paga. */
  @Transactional
  public VideoProductionCycleContracts.Response completeProviderPreflight(
      Long cycleId, VideoProviderPreflightContracts.ResultRequest request) {
    VideoProductionCycle cycle = cycle(cycleId);
    boolean preflightOnly = "PENDING_PROVIDER_PREFLIGHT_ONLY".equals(cycle.getStatus());
    if (!List.of(
            "PENDING_PROVIDER_PREFLIGHT",
            "PROVIDER_PREFLIGHT_BLOCKED",
            "PENDING_PROVIDER_PREFLIGHT_ONLY")
        .contains(cycle.getStatus())) {
      return response(cycle);
    }
    VideoProviderPreflight preflight =
        providerPreflightService.complete(cycle, providerPreflightResult(request));
    cycle.setUpdatedAt(Instant.now());
    if (!List.of("READY", "READY_WITH_BLOCKER").contains(preflight.getStatus())) {
      cycle.setStatus(
          preflightOnly ? "PROVIDER_PREFLIGHT_ONLY_BLOCKED" : "PROVIDER_PREFLIGHT_BLOCKED");
      return response(repository.save(cycle));
    }
    if (preflightOnly) {
      cycle.setStatus("PROVIDER_PREFLIGHT_ONLY_COMPLETED");
      return response(repository.save(cycle));
    }
    if ("READY".equals(preflight.getStatus())) {
      providerPreflightService.reserve(cycle);
    }
    if (cycle.getAgentTaskId() == null) {
      cycle.setAgentTaskId(createFinancialGate(cycle, project(cycle.getVideoProjectId())).id());
    }
    cycle.setStatus("PENDING_FINANCIAL_REVIEW");
    return response(repository.save(cycle));
  }

  /** Lista a fila canônica que Plutus pode avaliar. */
  @Transactional(readOnly = true)
  public List<VideoProductionCycleContracts.FinancialReviewPendingResponse>
      pendingFinancialReview() {
    return repository.findByStatusOrderByCreatedAtAsc("PENDING_FINANCIAL_REVIEW").stream()
        .map(this::financialReviewPendingResponse)
        .toList();
  }

  /** Audita a interação de Plutus sem permitir que o callback avance o ciclo. */
  @Transactional
  public void auditFinancialReview(
      Long cycleId, VideoProductionCycleContracts.FinancialReviewAuditRequest request) {
    VideoProductionCycle cycle = cycle(cycleId);
    if (!"PENDING_FINANCIAL_REVIEW".equals(cycle.getStatus()) || cycle.getAgentTaskId() == null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O ciclo não possui revisão financeira pendente.");
    }
    taskService.recordPendingGateModelResult(
        PLUTUS_KEY,
        cycle.getAgentTaskId(),
        request.rawModelResponse(),
        request.executionAudit(),
        request.modelUsages());
  }

  /** Aplica a decisão de Plutus e, quando aprovada, cria o job de Apolo. */
  @Transactional
  public VideoProductionCycleContracts.Response decide(
      Long id, VideoProductionCycleContracts.FinancialDecisionRequest request) {
    if (!PLUTUS_KEY.equals(request.decidedByAgentKey())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente Plutus decide este gate.");
    }
    VideoProductionCycle cycle = cycle(id);
    if (!"PENDING_FINANCIAL_REVIEW".equals(cycle.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O ciclo já recebeu decisão financeira.");
    }
    String decision = request.decision().trim().toUpperCase();
    if (!List.of("APPROVED", "REJECTED").contains(decision)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decisão financeira inválida.");
    }
    providerPreflightService.validateFinancialDecision(
        cycle, providerFinancialDecision(request), decision);
    if ("REJECTED".equals(decision)) {
      if (providerPreflightService.hasActiveReservation(cycle.getId())) {
        providerPreflightService.releaseUnusedReservation(cycle.getId());
      }
      recordFinancialDecision(cycle, decision, request);
      taskService.decideGate(
          cycle.getAgentTaskId(),
          new DecideAgentGateRequest(PLUTUS_KEY, decision, request.reason()));
      cycle.setStatus("FINANCIAL_BLOCKED");
      return response(repository.save(cycle));
    }
    providerPreflightService.reserve(cycle);
    recordFinancialDecision(cycle, decision, request);
    taskService.decideGate(
        cycle.getAgentTaskId(), new DecideAgentGateRequest(PLUTUS_KEY, decision, request.reason()));
    VideoProject project = project(cycle.getVideoProjectId());
    queueApollo(cycle, project, null);
    return response(repository.save(cycle));
  }

  /** Reconcilia somente filas sem job; falhas pagas exigem novo preflight e nunca geram retry. */
  @Transactional
  public void reconcileApolloQueue() {
    repository
        .findByStatusAndFinancialDecisionOrderByCreatedAtAsc("QUEUED_FOR_APOLLO", "APPROVED")
        .forEach(
            cycle -> {
              SalesVideoJob previous =
                  cycle.getSalesVideoJobId() == null
                      ? null
                      : jobRepository.findById(cycle.getSalesVideoJobId()).orElse(null);
              if (previous != null && previous.getStatus() != SalesVideoStatus.VIDEO_FAILED) return;
              if (previous != null) {
                recordApolloFailure(cycle, previous);
                if (reuseProducedMaterial(cycle, previous)) {
                  repository.save(cycle);
                  return;
                }
                cycle.setStatus(APOLLO_BLOCKED);
                cycle.setUpdatedAt(Instant.now());
                repository.save(cycle);
                return;
              }
              if (!providerPreflightService.hasActiveReservation(cycle.getId())) {
                cycle.setStatus(APOLLO_BLOCKED);
                cycle.setLastApolloFailureCode("PROVIDER_PREFLIGHT_REQUIRED");
                cycle.setLastApolloFailureDetail(
                    "Ciclo legado sem preflight e reserva vigentes; abra um novo ciclo pelo Estúdio.");
                cycle.setLastApolloFailureAt(Instant.now());
                cycle.setUpdatedAt(Instant.now());
                repository.save(cycle);
                return;
              }
              queueApollo(cycle, project(cycle.getVideoProjectId()), previous);
              repository.save(cycle);
            });
  }

  /** Reaproveita localmente o vídeo preservado antes de considerar qualquer nova geração paga. */
  private boolean reuseProducedMaterial(VideoProductionCycle cycle, SalesVideoJob failedJob) {
    if (failedJob.getAsset() == null
        || !"RENDER_DURATION_SHORT".equals(failedJob.getFailureCode())) {
      return false;
    }
    VideoProject project = project(cycle.getVideoProjectId());
    String caption = firstText(project.getCaptionPlan(), project.getCtaText());
    if (caption == null) {
      cycle.setStatus(APOLLO_BLOCKED);
      cycle.setLastApolloFailureDetail(
          "Material preservado, mas o plano comercial não possui texto aprovado para pós-produção.");
      cycle.setUpdatedAt(Instant.now());
      return true;
    }
    RequestSalesVideoPostProductionRequest request = new RequestSalesVideoPostProductionRequest();
    request.setRequestedBy("Apolo");
    request.setCaptionText(caption);
    Long postProductionJobId =
        jobRepository
            .findFirstByRetryOfJob_IdOrderByRequestedAtDesc(failedJob.getId())
            .map(SalesVideoJob::getId)
            .orElseGet(
                () -> salesVideoService.requestPostProduction(failedJob.getId(), request).getId());
    cycle.setSalesVideoJobId(postProductionJobId);
    cycle.setStatus("REUSING_APOLLO_MATERIAL");
    cycle.setUpdatedAt(Instant.now());
    return true;
  }

  /** Retorna o primeiro texto comercial preenchido sem criar copy nova durante a recuperação. */
  private String firstText(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) return value.trim();
    }
    return null;
  }

  /** Persiste o diagnóstico do job terminal antes de criar uma substituição segura. */
  private void recordApolloFailure(VideoProductionCycle cycle, SalesVideoJob failedJob) {
    cycle.setLastFailedJobId(failedJob.getId());
    cycle.setLastApolloFailureCode(failedJob.getFailureCode());
    cycle.setLastApolloFailureDetail(failedJob.getFailureDetail());
    cycle.setLastApolloFailureAt(
        failedJob.getFinishedAt() == null ? Instant.now() : failedJob.getFinishedAt());
  }

  /** Cria o job canônico de Apolo somente com reserva ativa e payload previamente validado. */
  private void queueApollo(
      VideoProductionCycle cycle, VideoProject project, SalesVideoJob previous) {
    providerPreflightService.requireActiveReservation(cycle.getId());
    String provider = isProductUgc(project.getProviderPlan()) ? RUNWAY_PRODUCT_UGC : RUNWAY_ROUTER;
    RequestVideoRenderRequest render = new RequestVideoRenderRequest();
    render.setRequestedBy("Apolo");
    render.setProviderFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
    render.setProviderName(provider);
    render.setExecutionMode(SalesVideoExecutionMode.TEST);
    render.setTargetDurationSeconds(
        RUNWAY_PRODUCT_UGC.equals(provider)
            ? project.getTargetDurationSeconds()
            : Math.min(10, project.getTargetDurationSeconds()));
    render.setMetadataJson(metadata(cycle, project, previous));
    SalesVideoJobDto job =
        salesVideoService.requestRender(project.getSalesVideoProfileId(), render);
    cycle.setSalesVideoJobId(job.getId());
    cycle.setStatus("QUEUED_FOR_APOLLO");
    cycle.setUpdatedAt(Instant.now());
  }

  /** Lista o histórico de ciclos do projeto para a tela. */
  @Transactional(readOnly = true)
  public List<VideoProductionCycleContracts.Response> list(Long projectId) {
    project(projectId);
    return repository.findByVideoProjectIdOrderByCreatedAtDesc(projectId).stream()
        .map(this::response)
        .toList();
  }

  /** Resolve o projeto requerido para o ciclo. */
  private VideoProject project(Long id) {
    return projectRepository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto não encontrado."));
  }

  /** Resolve o ciclo requerido. */
  private VideoProductionCycle cycle(Long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ciclo não encontrado."));
  }

  /** Identifica o modelo preferido apenas para dimensionar clipes antes do roteamento externo. */
  private String preferredProvider(VideoProject project) {
    String plan = project.getProviderPlan();
    if (isProductUgc(plan)) return RUNWAY_PRODUCT_UGC;
    if (plan != null && plan.contains("RUNWAY_SEEDANCE_2_5")) return "RUNWAY_SEEDANCE_2_5";
    if (plan != null && plan.contains("RUNWAY_HAILUO_3")) return "RUNWAY_HAILUO_3";
    return "RUNWAY_SEEDANCE_2_5";
  }

  /** Monta metadados auditáveis sem autorizar publicação. */
  private String metadata(
      VideoProductionCycle cycle, VideoProject project, SalesVideoJob previous) {
    try {
      LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
      VideoCreditReservation reservation =
          providerPreflightService.requireActiveReservation(cycle.getId());
      int duration = project.getTargetDurationSeconds();
      String provider = preferredProvider(project);
      boolean productUgc = RUNWAY_PRODUCT_UGC.equals(provider);
      int providerClipDuration = providerClipDurationSeconds(provider);
      List<LinkedHashMap<String, Object>> cuts =
          productUgc ? List.of() : cutPlan(project, duration);
      metadata.put("videoProductionCycleId", cycle.getId());
      metadata.put("videoProjectId", project.getId());
      metadata.put("productId", cycle.getProductId());
      metadata.put("commercialPlanId", cycle.getCommercialPlanId());
      metadata.put("experimentId", cycle.getExperimentId());
      metadata.put("budgetLimitUsd", cycle.getBudgetLimitUsd());
      metadata.put("financialApprovedBy", "Plutus");
      metadata.put("providerCreditReservationId", reservation.getId());
      metadata.put("providerReservedCredits", reservation.getReservedCredits());
      metadata.put("providerReservedCostUsd", reservation.getReservedCostUsd());
      metadata.put("providerReservationExpiresAt", reservation.getExpiresAt());
      metadata.put(
          "providerPreflightPayloadSha256", providerPreflightService.payloadSha256(cycle.getId()));
      metadata.put("runwayRouterConfigId", providerPreflightService.routerConfigId(cycle.getId()));
      metadata.put(
          "runwayRouterRequestsJson", providerPreflightService.executionRequests(cycle.getId()));
      metadata.put(
          "runwaySelectedRoutesJson", providerPreflightService.selectedRoutes(cycle.getId()));
      metadata.put("publicationAllowed", false);
      metadata.put("targetDurationSeconds", duration);
      metadata.put("providerClipDurationSeconds", providerClipDuration);
      metadata.put(
          "sceneCount",
          productUgc ? 1 : (duration + providerClipDuration - 1) / providerClipDuration);
      metadata.put(
          "cutCount", productUgc ? captionSegments(project.getCaptionPlan()) : cuts.size());
      metadata.put("assemblyRequired", !productUgc && duration > providerClipDuration);
      metadata.put(
          "generation_strategy",
          productUgc
              ? "RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION"
              : "PROVIDER_CLIPS_WITH_POST_PRODUCTION_CUTS");
      if (researchIntelligenceMapper != null) {
        metadata.put(
            "researchIntelligence",
            researchIntelligenceMapper.selectForVideoAgent(project, APOLLO_KEY));
      }
      metadata.put("cut_plan", cuts);
      LinkedHashMap<String, Object> postProduction = new LinkedHashMap<>();
      postProduction.put("text_rendering", "DETERMINISTIC_OVERLAY");
      postProduction.put("provider_embedded_text_allowed", false);
      postProduction.put("caption_plan", nullToEmpty(project.getCaptionPlan()));
      postProduction.put("cta_text", nullToEmpty(project.getCtaText()));
      postProduction.put("editing_notes", nullToEmpty(project.getEditingNotes()));
      metadata.put("post_production", postProduction);
      if (productUgc) {
        LinkedHashMap<String, Object> technicalGate = new LinkedHashMap<>();
        technicalGate.put("continuousTakeRequired", true);
        technicalGate.put("maximumMeanMotionDelta", new BigDecimal("1.25"));
        technicalGate.put("maximumPeakMotionDelta", new BigDecimal("12.0"));
        technicalGate.put("forbidMirrorOrReflection", true);
        technicalGate.put("captionMustMatchNarration", true);
        metadata.put("technicalQualityGate", technicalGate);
        LinkedHashMap<String, Object> referenceGovernance = new LinkedHashMap<>();
        referenceGovernance.put(
            "presenterConsentEvidence", project.getPerformanceConsentEvidence().trim());
        referenceGovernance.put(
            "referenceRightsEvidence", project.getPerformanceRightsEvidence().trim());
        referenceGovernance.put("productIsDigitalExperience", true);
        metadata.put("referenceGovernance", referenceGovernance);
        LinkedHashMap<String, Object> finalization = new LinkedHashMap<>();
        finalization.put("enabled", true);
        finalization.put("voiceOverScript", narrationFromCaption(project.getCaptionPlan()));
        finalization.put("captionText", project.getCaptionPlan().trim());
        finalization.put("soundtrackPlan", nullToEmpty(project.getSoundtrackPlan()));
        finalization.put("ctaText", nullToEmpty(project.getCtaText()));
        finalization.put("requiredReviewers", List.of("Psique", "Temis", "HUMAN"));
        metadata.put("premiumFinalization", finalization);
      }
      if (previous != null) metadata.put("replacesFailedJobId", previous.getId());
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao serializar contrato do ciclo premium; cycleId={}", cycle.getId(), ex);
      throw new IllegalStateException("Não foi possível auditar o ciclo de vídeo.", ex);
    }
  }

  /** Resolve a duração de geração por modelo sem tratá-la como duração de cada corte editorial. */
  private int providerClipDurationSeconds(String provider) {
    if (RUNWAY_PRODUCT_UGC.equals(provider)) return 15;
    if (provider != null && provider.contains("SEEDANCE_2")) return 15;
    if (provider != null && provider.contains("VEO_3_1")) return 8;
    return 10;
  }

  /** Valida o contrato da receita premium antes de abrir preflight ou tarefa de agente. */
  private void validateProviderPlan(VideoProject project) {
    if (!isProductUgc(project.getProviderPlan())) return;
    Integer duration = project.getTargetDurationSeconds();
    if (duration == null || duration < 4 || duration > 15) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Product UGC exige duração entre 4 e 15 segundos.");
    }
    if (!"image".equalsIgnoreCase(project.getCharacterPerformanceType())
        || !isHttps(project.getCharacterPerformanceUri())
        || !isHttps(project.getReferencePerformanceUri())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Product UGC exige imagem HTTPS da apresentadora e tela HTTPS do PDE.");
    }
    if (project.getPerformanceConsentEvidence() == null
        || project.getPerformanceConsentEvidence().isBlank()
        || project.getPerformanceRightsEvidence() == null
        || project.getPerformanceRightsEvidence().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Product UGC exige consentimento e direitos auditáveis das referências.");
    }
    if (project.getCaptionPlan() == null
        || project.getCaptionPlan().isBlank()
        || project.getCtaText() == null
        || project.getCtaText().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Product UGC exige texto final e CTA aprovados.");
    }
  }

  /** Identifica a escolha explícita da receita no plano persistido do projeto. */
  private boolean isProductUgc(String providerPlan) {
    return providerPlan != null
        && providerPlan.toUpperCase(java.util.Locale.ROOT).contains("(RUNWAY_PRODUCT_UGC)");
  }

  /** Confirma referência pública HTTPS antes de entregar o contrato ao executor. */
  private boolean isHttps(String value) {
    if (value == null || value.isBlank()) return false;
    try {
      java.net.URI uri = java.net.URI.create(value.trim());
      return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
    } catch (IllegalArgumentException ex) {
      log.warn("Referência Product UGC inválida; value={}", value, ex);
      return false;
    }
  }

  /** Conta os trechos que usarão a mesma linha narrativa na voz e na legenda. */
  private int captionSegments(String captionPlan) {
    if (captionPlan == null || captionPlan.isBlank()) return 0;
    return (int)
        java.util.Arrays.stream(captionPlan.split("\\s*\\|\\s*"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .count();
  }

  /** Deriva a narração do texto exibido, sem permitir uma segunda copy divergente. */
  private String narrationFromCaption(String captionPlan) {
    return java.util.Arrays.stream(captionPlan.split("\\s*\\|\\s*"))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .collect(java.util.stream.Collectors.joining(" "));
  }

  /** Cria cortes comerciais curtos que serão agrupados nos clipes cobrados pelo provider. */
  private List<LinkedHashMap<String, Object>> cutPlan(VideoProject project, int duration) {
    int cutCount = Math.max(4, Math.min(48, (int) Math.ceil(duration / 4.0)));
    int baseDuration = duration / cutCount;
    int remainder = duration % cutCount;
    List<String> sceneObjectives = sceneObjectives(project.getScenePlan());
    List<LinkedHashMap<String, Object>> cuts = new ArrayList<>();
    for (int index = 0; index < cutCount; index++) {
      LinkedHashMap<String, Object> cut = new LinkedHashMap<>();
      cut.put("order", index + 1);
      cut.put("duration_seconds", baseDuration + (index < remainder ? 1 : 0));
      cut.put("role", cutRole(index, cutCount));
      cut.put("narrative_phase", narrativePhase(index, cutCount));
      cut.put("visual_objective", cutObjective(index, cutCount, sceneObjectives));
      cut.put("continuity_anchor", "Mesma personagem, figurino, ambiente e luz do plano anterior.");
      cut.put("source_scene_plan", nullToEmpty(project.getScenePlan()));
      cuts.add(cut);
    }
    return cuts;
  }

  /** Distribui dor, resultado, mecanismo, prova, oferta e CTA ao longo do plano de cortes. */
  private String cutRole(int index, int count) {
    if (index == 0) return "HOOK_DOR";
    if (index == count - 1) return "CTA";
    if (index >= count - 2) return "PROVA";
    if (index >= Math.max(2, count / 2)) return "RESULTADO";
    return "MECANISMO";
  }

  /** Distribui os cortes em uma progressão que não retorna a fases narrativas anteriores. */
  private String narrativePhase(int index, int count) {
    if (index == 0) return "HOOK";
    if (index == 1) return "SETUP";
    if (index == count - 1) return "CTA";
    if (index == count - 2) return "PROOF";
    if (index >= Math.max(3, count / 2)) return "TRANSFORMATION";
    if (index == 2) return "DISCOVERY";
    return "DEMONSTRATION";
  }

  /** Define uma ação visual única para impedir clipes longos, genéricos ou repetitivos. */
  private String cutObjective(int index, int count, List<String> sceneObjectives) {
    if (!sceneObjectives.isEmpty()) {
      int sourceIndex =
          Math.min(
              sceneObjectives.size() - 1,
              (int) Math.floor(index * sceneObjectives.size() / (double) count));
      return "Beat editorial %d/%d: %s"
          .formatted(index + 1, count, sceneObjectives.get(sourceIndex));
    }
    if (index == 0) return "Abrir com dor reconhecível e ação imediata, sem texto embutido.";
    if (index == 1) return "Situar a dor no cotidiano e criar expectativa para a descoberta.";
    if (index == count - 1)
      return "Encerrar com gesto de decisão e área limpa para CTA em pós-produção.";
    if (index == count - 2)
      return "Mostrar prova ou entregável concreto sem interface ou letras geradas.";
    if (index >= Math.max(3, count / 2))
      return "Mostrar transformação plausível causada pelas microações já demonstradas.";
    return "Demonstrar uma única microação do mecanismo, preservando continuidade visual.";
  }

  /** Extrai cenas persistidas para que o plano automático não substitua a receita aprovada. */
  private List<String> sceneObjectives(String scenePlan) {
    if (scenePlan == null || scenePlan.isBlank()) {
      return List.of();
    }
    return scenePlan.lines().map(String::trim).filter(value -> !value.isBlank()).limit(48).toList();
  }

  /** Normaliza campos opcionais do plano usados na auditoria de pós-produção. */
  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  /** Converte o contrato HTTP validado no dado interno consumido pelo preflight financeiro. */
  private VideoProviderFinancialPreflightData.Result providerPreflightResult(
      VideoProviderPreflightContracts.ResultRequest request) {
    return new VideoProviderFinancialPreflightData.Result(
        request.status(),
        request.accountKey(),
        request.routerConfigId(),
        request.payloadSha256(),
        request.executionRequestsJson(),
        request.organizationSnapshotJson(),
        request.routingResponseJson(),
        request.selectedRoutesJson(),
        request.estimatedCredits(),
        request.officialBalanceCredits(),
        request.maxMonthlyCreditSpend(),
        request.quotaSnapshotJson(),
        request.usageSnapshotJson(),
        request.failureCode(),
        request.failureDetail(),
        request.sourceUrl(),
        request.observedAt());
  }

  /** Converte a decisão HTTP de Plutus no subconjunto necessário ao domínio financeiro. */
  private VideoProviderFinancialPreflightData.FinancialDecision providerFinancialDecision(
      VideoProductionCycleContracts.FinancialDecisionRequest request) {
    return new VideoProviderFinancialPreflightData.FinancialDecision(
        request.recommendedAggregator(),
        request.recommendedRoute(),
        request.estimatedCostUsd(),
        request.creditAction(),
        request.recommendedRechargeCredits(),
        request.rechargeUrl());
  }

  /** Converte a pendência interna no contrato exposto ao executor de vídeo. */
  private VideoProviderPreflightContracts.PendingResponse providerPreflightPendingResponse(
      VideoProviderFinancialPreflightData.Pending value) {
    return new VideoProviderPreflightContracts.PendingResponse(
        value.preflightId(),
        value.cycleId(),
        value.aggregatorName(),
        value.accountKey(),
        value.productionProfile(),
        value.providerPlan(),
        value.maxCredits(),
        value.targetDurationSeconds(),
        value.providerClipDurationSeconds(),
        value.generationClipCount(),
        value.aspectRatio(),
        value.resolution(),
        value.audio(),
        value.title(),
        value.objective(),
        value.hookText(),
        value.scriptText(),
        value.scenePlan(),
        value.characterBible(),
        value.environmentBible(),
        value.objectBible(),
        value.visualStyleGuide(),
        value.continuityRules(),
        value.captionPlan(),
        value.ctaText(),
        value.characterPerformanceType(),
        value.characterPerformanceUri(),
        value.referencePerformanceUri(),
        value.performanceConsentEvidence(),
        value.performanceRightsEvidence(),
        value.editingNotes(),
        value.qualityGate(),
        value.learningObjective(),
        value.successCriterion());
  }

  /** Converte snapshot e reserva internos para o relatório público do ciclo. */
  private VideoProviderPreflightContracts.SnapshotResponse providerPreflightSnapshot(Long cycleId) {
    VideoProviderFinancialPreflightData.Snapshot value = providerPreflightService.snapshot(cycleId);
    if (value == null) return null;
    return new VideoProviderPreflightContracts.SnapshotResponse(
        value.id(),
        value.status(),
        value.productionProfile(),
        value.aggregatorName(),
        value.accountKey(),
        value.routerConfigId(),
        value.payloadSha256(),
        value.selectedRoutesJson(),
        value.estimatedCredits(),
        value.estimatedCostUsd(),
        value.maximumAuthorizedCredits(),
        value.maximumAuthorizedCostUsd(),
        value.officialBalanceCredits(),
        value.reservedCreditsSnapshot(),
        value.availableCreditsSnapshot(),
        value.maxMonthlyCreditSpend(),
        value.quotaSnapshotJson(),
        value.failureCode(),
        value.failureDetail(),
        value.sourceUrl(),
        value.rechargeUrl(),
        value.observedAt(),
        value.expiresAt(),
        providerReservation(value.reservation()));
  }

  /** Converte a reserva interna no contrato sanitizado exibido pelo Marketing Hub. */
  private VideoProviderPreflightContracts.ReservationResponse providerReservation(
      VideoProviderFinancialPreflightData.Reservation value) {
    if (value == null) return null;
    return new VideoProviderPreflightContracts.ReservationResponse(
        value.id(),
        value.status(),
        value.reservedCredits(),
        value.reservedCostUsd(),
        value.actualCredits(),
        value.actualCostUsd(),
        value.expiresAt(),
        value.reservedAt(),
        value.settledAt(),
        value.releasedAt());
  }

  /** Converte a entidade no contrato externo. */
  private VideoProductionCycleContracts.Response response(VideoProductionCycle cycle) {
    VideoProject project = project(cycle.getVideoProjectId());
    int duration = project.getTargetDurationSeconds();
    boolean productUgc = RUNWAY_PRODUCT_UGC.equals(preferredProvider(project));
    int providerClipDuration = providerClipDurationSeconds(preferredProvider(project));
    return new VideoProductionCycleContracts.Response(
        cycle.getId(),
        cycle.getVideoProjectId(),
        cycle.getProductId(),
        cycle.getCommercialPlanId(),
        cycle.getExperimentId(),
        cycle.getStatus(),
        cycle.getBudgetLimitUsd(),
        cycle.getKnownCostUsd(),
        cycle.getLearningObjective(),
        cycle.getSuccessCriterion(),
        providerPreflightSnapshot(cycle.getId()),
        financialSnapshot(cycle),
        cycle.getFinancialDecision(),
        cycle.getFinancialReason(),
        cycle.getRecommendedAggregator(),
        cycle.getRecommendedRoute(),
        cycle.getEstimatedCostUsd(),
        cycle.getCostBenefitBasis(),
        cycle.getCreditAction(),
        cycle.getRecommendedRechargeCredits(),
        cycle.getRechargeUrl(),
        cycle.getSalesVideoJobId(),
        cycle.getLastFailedJobId(),
        cycle.getLastApolloFailureCode(),
        cycle.getLastApolloFailureDetail(),
        cycle.getLastApolloFailureAt(),
        cycle.getMonitoredTaskCount(),
        cycle.getMonitoredCredits(),
        cycle.getBudgetMonitorStatus(),
        cycle.getBudgetAlertCode(),
        cycle.getBudgetAlertDetail(),
        cycle.getBudgetAlertAt(),
        providerClipDuration,
        productUgc ? 1 : (duration + providerClipDuration - 1) / providerClipDuration,
        productUgc ? captionSegments(project.getCaptionPlan()) : cutPlan(project, duration).size(),
        !productUgc && duration > providerClipDuration,
        cycle.getAgentTaskId(),
        cycle.getCreatedAt(),
        cycle.getUpdatedAt());
  }

  /** Monta a fila interna de Plutus sem carregar a resposta bruta nas telas administrativas. */
  private VideoProductionCycleContracts.FinancialReviewPendingResponse
      financialReviewPendingResponse(VideoProductionCycle cycle) {
    return new VideoProductionCycleContracts.FinancialReviewPendingResponse(
        cycle.getId(),
        cycle.getVideoProjectId(),
        cycle.getProductId(),
        cycle.getCommercialPlanId(),
        cycle.getExperimentId(),
        cycle.getStatus(),
        cycle.getBudgetLimitUsd(),
        cycle.getKnownCostUsd(),
        financialSnapshot(cycle),
        cycle.getAgentTaskId(),
        cycle.getAgentTaskId() == null
            ? null
            : taskService.pendingGateModelResult(PLUTUS_KEY, cycle.getAgentTaskId()));
  }

  /** Congela para Plutus a mesma inteligência financeira oficial do planejamento. */
  private String financialSnapshot(VideoProductionCycle cycle) {
    try {
      java.util.LinkedHashMap<String, Object> snapshot =
          new java.util.LinkedHashMap<>(
              cycle.getCommercialPlanId() == null
                  ? financialAgentService.unassignedStudioIntelligence(cycle.getProductId())
                  : financialAgentService.intelligence(cycle.getCommercialPlanId()));
      snapshot.put("learningObjective", cycle.getLearningObjective());
      snapshot.put("successCriterion", cycle.getSuccessCriterion());
      snapshot.put("incrementalLedger", studioCostLedgerService.cycleLedger(cycle.getId()));
      snapshot.putAll(providerPreflightService.financialContext(cycle.getId()));
      return objectMapper.writeValueAsString(snapshot);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao congelar contexto financeiro de Plutus; cycleId={}", cycle.getId(), ex);
      throw new IllegalStateException("Não foi possível congelar o contexto financeiro.", ex);
    }
  }

  /** Cria o gate financeiro somente depois de saldo, quota e custo do payload existirem. */
  private AgentTaskResponse createFinancialGate(VideoProductionCycle cycle, VideoProject project) {
    return taskService.createGateByAgent(
        new CreateAgentTaskByAgentRequest(
            APOLLO_KEY,
            PLUTUS_KEY,
            "Avaliar custo-benefício do ciclo de vídeo #" + cycle.getId(),
            "Comparar rota, saldo, quota, custo previsto e histórico para o projeto "
                + project.getTitle()
                + ". Nenhuma chamada paga ao provider pode ocorrer antes da reserva.",
            "HIGH",
            "video-production-cycle:" + cycle.getId()),
        "VIDEO_PROVIDER_COST_BENEFIT_APPROVAL");
  }

  /** Persiste a decisão financeira antes de qualquer transição de estado do ciclo. */
  private void recordFinancialDecision(
      VideoProductionCycle cycle,
      String decision,
      VideoProductionCycleContracts.FinancialDecisionRequest request) {
    cycle.setFinancialDecision(decision);
    cycle.setFinancialReason(request.reason().trim());
    cycle.setRecommendedAggregator(request.recommendedAggregator());
    cycle.setRecommendedRoute(request.recommendedRoute());
    cycle.setEstimatedCostUsd(request.estimatedCostUsd());
    cycle.setCostBenefitBasis(request.costBenefitBasis());
    cycle.setCreditAction(request.creditAction());
    cycle.setRecommendedRechargeCredits(request.recommendedRechargeCredits());
    cycle.setRechargeUrl(request.rechargeUrl());
    cycle.setFinancialDecidedAt(Instant.now());
    cycle.setUpdatedAt(Instant.now());
  }
}
