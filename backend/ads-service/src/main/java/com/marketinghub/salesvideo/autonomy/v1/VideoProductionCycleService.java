package com.marketinghub.salesvideo.autonomy.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskByAgentRequest;
import com.marketinghub.agenttask.DecideAgentGateRequest;
import com.marketinghub.financialagent.service.FinancialAgentService;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoExecutionMode;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import com.marketinghub.salesvideo.VideoProductionCycle;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.dto.RequestVideoRenderRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.service.SalesVideoService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: coordenar o gate de Plutus e a entrega de ciclos aprovados a Apolo. */
@Service
public class VideoProductionCycleService {
  private static final String PLUTUS_KEY = "financial-agent";
  private static final String APOLLO_KEY = "videomaker";
  private final VideoProductionCycleRepository repository;
  private final VideoProjectRepository projectRepository;
  private final AgentTaskService taskService;
  private final SalesVideoService salesVideoService;
  private final FinancialAgentService financialAgentService;
  private final StudioCostLedgerService studioCostLedgerService;
  private final ObjectMapper objectMapper;

  /** Configura persistência, caixas de entrada e o executor canônico de vídeo. */
  public VideoProductionCycleService(
      VideoProductionCycleRepository repository,
      VideoProjectRepository projectRepository,
      AgentTaskService taskService,
      SalesVideoService salesVideoService,
      FinancialAgentService financialAgentService,
      StudioCostLedgerService studioCostLedgerService,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.projectRepository = projectRepository;
    this.taskService = taskService;
    this.salesVideoService = salesVideoService;
    this.financialAgentService = financialAgentService;
    this.studioCostLedgerService = studioCostLedgerService;
    this.objectMapper = objectMapper;
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
    RequestVideoRenderRequest render = new RequestVideoRenderRequest();
    render.setRequestedBy("Apolo");
    render.setProviderFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
    render.setProviderName(preferredProvider(project));
    render.setExecutionMode(SalesVideoExecutionMode.TEST);
    render.setTargetDurationSeconds(project.getTargetDurationSeconds());
    render.setMetadataJson(metadata(cycle, project));
    SalesVideoJobDto job =
        salesVideoService.requestRender(project.getSalesVideoProfileId(), render);
    cycle.setSalesVideoJobId(job.getId());
    cycle.setStatus("QUEUED_FOR_APOLLO");
    return response(repository.save(cycle));
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

  /** Seleciona somente um provider já suportado pelo executor atual. */
  private String preferredProvider(VideoProject project) {
    String plan = project.getProviderPlan();
    if (plan != null && (plan.toUpperCase().contains("LUMA") || plan.contains("RAY_3_2"))) {
      return "LUMA_RAY_3_2";
    }
    if (plan != null && plan.contains("RUNWAY_SEEDANCE_2_5")) return "RUNWAY_SEEDANCE_2_5";
    if (plan != null && plan.contains("RUNWAY_HAILUO_3")) return "RUNWAY_HAILUO_3";
    if (project.getTargetDurationSeconds() != null && project.getTargetDurationSeconds() > 10) {
      return "LUMA_RAY_3_2";
    }
    return "RUNWAY_GEN_4_TURBO";
  }

  /** Monta metadados auditáveis sem autorizar publicação. */
  private String metadata(VideoProductionCycle cycle, VideoProject project) {
    try {
      return objectMapper.writeValueAsString(
          java.util.Map.of(
              "videoProductionCycleId", cycle.getId(),
              "videoProjectId", project.getId(),
              "budgetLimitUsd", cycle.getBudgetLimitUsd(),
              "financialApprovedBy", "Plutus",
              "publicationAllowed", false));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Não foi possível auditar o ciclo de vídeo.", ex);
    }
  }

  /** Converte a entidade no contrato externo. */
  private VideoProductionCycleContracts.Response response(VideoProductionCycle cycle) {
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
