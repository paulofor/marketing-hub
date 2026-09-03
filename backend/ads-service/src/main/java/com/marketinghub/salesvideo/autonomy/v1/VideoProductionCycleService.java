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
import com.marketinghub.salesvideo.VideoProductionCycle;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.dto.RequestSalesVideoPostProductionRequest;
import com.marketinghub.salesvideo.dto.RequestVideoRenderRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.mapper.VideoProjectResearchIntelligenceMapper;
import com.marketinghub.salesvideo.service.SalesVideoService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: coordenar o gate de Plutus e a entrega de ciclos aprovados a Apolo. */
@Service
public class VideoProductionCycleService {
  private static final String PLUTUS_KEY = "financial-agent";
  private static final String APOLLO_KEY = "videomaker";
  private static final String MUSA_PROVIDER = "RUNWAY_SEEDANCE_2_5";
  private static final String APOLLO_BLOCKED = "APOLLO_BLOCKED";
  private final VideoProductionCycleRepository repository;
  private final VideoProjectRepository projectRepository;
  private final SalesVideoJobRepository jobRepository;
  private final AgentTaskService taskService;
  private final SalesVideoService salesVideoService;
  private final FinancialAgentService financialAgentService;
  private final StudioCostLedgerService studioCostLedgerService;
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
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.projectRepository = projectRepository;
    this.jobRepository = jobRepository;
    this.taskService = taskService;
    this.salesVideoService = salesVideoService;
    this.financialAgentService = financialAgentService;
    this.studioCostLedgerService = studioCostLedgerService;
    this.objectMapper = objectMapper;
  }

  /** Conecta a biblioteca comum usada para auditar o contexto entregue a Apolo. */
  @Autowired
  public void setResearchIntelligenceMapper(
      VideoProjectResearchIntelligenceMapper researchIntelligenceMapper) {
    this.researchIntelligenceMapper = researchIntelligenceMapper;
  }

  /** Abre um ciclo bloqueado e solicita a avaliação financeira de Plutus. */
  @Transactional
  public VideoProductionCycleContracts.Response create(
      VideoProductionCycleContracts.CreateRequest request) {
    VideoProject project = project(request.videoProjectId());
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
    cycle.setStatus("PENDING_FINANCIAL_REVIEW");
    cycle.setBudgetLimitUsd(request.budgetLimitUsd());
    cycle.setKnownCostUsd(BigDecimal.ZERO);
    cycle.setLearningObjective(request.learningObjective().trim());
    cycle.setSuccessCriterion(request.successCriterion().trim());
    cycle.setCreatedAt(now);
    cycle.setUpdatedAt(now);
    cycle = repository.save(cycle);
    AgentTaskResponse task =
        taskService.createGateByAgent(
            new CreateAgentTaskByAgentRequest(
                APOLLO_KEY,
                PLUTUS_KEY,
                "Avaliar orçamento do ciclo de vídeo #" + cycle.getId(),
                "Validar teto de US$ "
                    + cycle.getBudgetLimitUsd()
                    + " para o projeto "
                    + project.getTitle()
                    + ". Nenhum provider pode ser acionado antes da aprovação.",
                "HIGH",
                "video-production-cycle:" + cycle.getId()),
            "VIDEO_BUDGET_APPROVAL");
    cycle.setAgentTaskId(task.id());
    return response(repository.save(cycle));
  }

  /** Lista a fila canônica que Plutus pode avaliar. */
  @Transactional(readOnly = true)
  public List<VideoProductionCycleContracts.Response> pendingFinancialReview() {
    return repository.findByStatusOrderByCreatedAtAsc("PENDING_FINANCIAL_REVIEW").stream()
        .map(this::response)
        .toList();
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
    cycle.setFinancialDecision(decision);
    cycle.setFinancialReason(request.reason().trim());
    cycle.setFinancialDecidedAt(Instant.now());
    cycle.setUpdatedAt(Instant.now());
    taskService.decideGate(
        cycle.getAgentTaskId(), new DecideAgentGateRequest(PLUTUS_KEY, decision, request.reason()));
    if ("REJECTED".equals(decision)) {
      cycle.setStatus("FINANCIAL_BLOCKED");
      return response(repository.save(cycle));
    }
    VideoProject project = project(cycle.getVideoProjectId());
    queueApollo(cycle, project, null);
    return response(repository.save(cycle));
  }

  /** Reconcilia ciclos aprovados cujo job terminal falhou, sem reabrir o gate de Plutus. */
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
                if (mustBlockAutomaticReplacement(previous)) {
                  cycle.setStatus(APOLLO_BLOCKED);
                  cycle.setUpdatedAt(Instant.now());
                  repository.save(cycle);
                  return;
                }
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

  /**
   * Interrompe consumo após rejeição financeira/não recuperável ou depois da primeira substituição
   * automática; material já renderizado deve seguir para avaliação, nunca para descarte e novo
   * gasto.
   */
  private boolean mustBlockAutomaticReplacement(SalesVideoJob failedJob) {
    String code = failedJob.getFailureCode() == null ? "" : failedJob.getFailureCode();
    String detail = failedJob.getFailureDetail() == null ? "" : failedJob.getFailureDetail();
    String metadata = failedJob.getMetadataJson() == null ? "" : failedJob.getMetadataJson();
    String provider = failedJob.getProviderName() == null ? "" : failedJob.getProviderName();
    if (provider.toUpperCase(java.util.Locale.ROOT).contains("LUMA")
        && !metadata.contains("\"replacesFailedJobId\"")) {
      return false;
    }
    return failedJob.getAsset() != null
        || metadata.contains("\"replacesFailedJobId\"")
        || code.contains("PAYMENT")
        || code.contains("CREDIT")
        || detail.contains("retryable=false")
        || detail.toLowerCase(java.util.Locale.ROOT).contains("not enough credits");
  }

  /** Persiste o diagnóstico do job terminal antes de criar uma substituição segura. */
  private void recordApolloFailure(VideoProductionCycle cycle, SalesVideoJob failedJob) {
    cycle.setLastFailedJobId(failedJob.getId());
    cycle.setLastApolloFailureCode(failedJob.getFailureCode());
    cycle.setLastApolloFailureDetail(failedJob.getFailureDetail());
    cycle.setLastApolloFailureAt(
        failedJob.getFinishedAt() == null ? Instant.now() : failedJob.getFinishedAt());
  }

  /** Cria o job canônico de Apolo com plano de cenas e rastreabilidade do job substituído. */
  private void queueApollo(
      VideoProductionCycle cycle, VideoProject project, SalesVideoJob previous) {
    RequestVideoRenderRequest render = new RequestVideoRenderRequest();
    render.setRequestedBy("Apolo");
    render.setProviderFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
    render.setProviderName(preferredProvider(project));
    render.setExecutionMode(SalesVideoExecutionMode.TEST);
    render.setTargetDurationSeconds(Math.min(10, project.getTargetDurationSeconds()));
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

  /** Seleciona o provider permitido pelo plano comercial, sem reintroduzir Luma. */
  private String preferredProvider(VideoProject project) {
    String plan = project.getProviderPlan();
    if (plan != null && plan.contains("RUNWAY_SEEDANCE_2_5")) return "RUNWAY_SEEDANCE_2_5";
    if (plan != null && plan.contains("RUNWAY_HAILUO_3")) return "RUNWAY_HAILUO_3";
    return MUSA_PROVIDER;
  }

  /** Monta metadados auditáveis sem autorizar publicação. */
  private String metadata(
      VideoProductionCycle cycle, VideoProject project, SalesVideoJob previous) {
    try {
      LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
      int duration = project.getTargetDurationSeconds();
      int providerClipDuration = providerClipDurationSeconds(preferredProvider(project));
      List<LinkedHashMap<String, Object>> cuts = cutPlan(project, duration);
      metadata.put("videoProductionCycleId", cycle.getId());
      metadata.put("videoProjectId", project.getId());
      metadata.put("budgetLimitUsd", cycle.getBudgetLimitUsd());
      metadata.put("financialApprovedBy", "Plutus");
      metadata.put("publicationAllowed", false);
      metadata.put("targetDurationSeconds", duration);
      metadata.put("providerClipDurationSeconds", providerClipDuration);
      metadata.put("sceneCount", (duration + providerClipDuration - 1) / providerClipDuration);
      metadata.put("cutCount", cuts.size());
      metadata.put("assemblyRequired", duration > providerClipDuration);
      metadata.put("generation_strategy", "PROVIDER_CLIPS_WITH_POST_PRODUCTION_CUTS");
      if (researchIntelligenceMapper != null) {
        metadata.put(
            "researchIntelligence",
            researchIntelligenceMapper.selectForVideoAgent(project, APOLLO_KEY));
      }
      metadata.put("cut_plan", cuts);
      metadata.put(
          "post_production",
          java.util.Map.of(
              "text_rendering", "DETERMINISTIC_OVERLAY",
              "provider_embedded_text_allowed", false,
              "caption_plan", nullToEmpty(project.getCaptionPlan()),
              "cta_text", nullToEmpty(project.getCtaText()),
              "editing_notes", nullToEmpty(project.getEditingNotes())));
      if (previous != null) metadata.put("replacesFailedJobId", previous.getId());
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Não foi possível auditar o ciclo de vídeo.", ex);
    }
  }

  /** Resolve a duração de geração por modelo sem tratá-la como duração de cada corte editorial. */
  private int providerClipDurationSeconds(String provider) {
    if (provider != null && provider.contains("SEEDANCE_2")) return 15;
    if (provider != null && provider.contains("VEO_3_1")) return 8;
    return 10;
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

  /** Converte a entidade no contrato externo. */
  private VideoProductionCycleContracts.Response response(VideoProductionCycle cycle) {
    VideoProject project = project(cycle.getVideoProjectId());
    int duration = project.getTargetDurationSeconds();
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
        financialSnapshot(cycle),
        cycle.getFinancialDecision(),
        cycle.getFinancialReason(),
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
        (duration + providerClipDuration - 1) / providerClipDuration,
        cutPlan(project, duration).size(),
        true,
        cycle.getAgentTaskId(),
        cycle.getCreatedAt(),
        cycle.getUpdatedAt());
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
      return objectMapper.writeValueAsString(snapshot);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Não foi possível congelar o contexto financeiro.", ex);
    }
  }
}
