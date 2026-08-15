package com.marketinghub.planning.web;

import com.marketinghub.planning.CommercialPlanStatus;
import com.marketinghub.planning.dto.CommercialPlanAgentActivityDto;
import com.marketinghub.planning.dto.CommercialPlanDto;
import com.marketinghub.planning.dto.CommercialPlanJourneyHomologationDto;
import com.marketinghub.planning.dto.CommercialPlanMilestoneDto;
import com.marketinghub.planning.dto.CommercialPlanOperationalFlowDto;
import com.marketinghub.planning.dto.CommercialPlanSimulationDto;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.dto.CommercialPlanVisualAssetDto;
import com.marketinghub.planning.dto.CommercialPlanWeekDto;
import com.marketinghub.planning.dto.CommercialPlanWeekObjectiveDto;
import com.marketinghub.planning.dto.CreateCommercialPlanRequest;
import com.marketinghub.planning.dto.CreateCommercialPlanSimulationRequest;
import com.marketinghub.planning.dto.CreateCommercialPlanVisualAssetRequest;
import com.marketinghub.planning.dto.UpdateCommercialPlanMilestoneRequest;
import com.marketinghub.planning.dto.UpdateCommercialPlanRequest;
import com.marketinghub.planning.dto.UpdateCommercialPlanVisualAssetStatusRequest;
import com.marketinghub.planning.dto.UpdateCommercialPlanWeekCommitmentStatusRequest;
import com.marketinghub.planning.dto.UpdateCommercialPlanWeekObjectivesRequest;
import com.marketinghub.planning.mapper.CommercialPlanMapper;
import com.marketinghub.planning.service.CommercialPlanAgentActivityService;
import com.marketinghub.planning.service.CommercialPlanJourneyHomologationService;
import com.marketinghub.planning.service.CommercialPlanOperationalFlowService;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.planning.service.CommercialPlanVisualAssetService;
import com.marketinghub.planning.service.CommercialPlanWeeklyExperimentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor endpoints do modulo de planejamento comercial. */
@RestController
@RequestMapping("/api/planning/commercial-plans")
public class CommercialPlanController {
  private final CommercialPlanService service;
  private final CommercialPlanWeeklyExperimentService weeklyExperimentService;
  private final CommercialPlanMapper mapper;
  private final CommercialPlanVersionService versionService;
  private final CommercialPlanAgentActivityService agentActivityService;
  private final CommercialPlanJourneyHomologationService journeyHomologationService;
  private final CommercialPlanOperationalFlowService operationalFlowService;
  private final CommercialPlanVisualAssetService visualAssetService;

  public CommercialPlanController(
      CommercialPlanService service,
      CommercialPlanWeeklyExperimentService weeklyExperimentService,
      CommercialPlanMapper mapper,
      CommercialPlanVersionService versionService,
      CommercialPlanAgentActivityService agentActivityService,
      CommercialPlanJourneyHomologationService journeyHomologationService,
      CommercialPlanOperationalFlowService operationalFlowService,
      CommercialPlanVisualAssetService visualAssetService) {
    this.service = service;
    this.weeklyExperimentService = weeklyExperimentService;
    this.mapper = mapper;
    this.versionService = versionService;
    this.agentActivityService = agentActivityService;
    this.journeyHomologationService = journeyHomologationService;
    this.operationalFlowService = operationalFlowService;
    this.visualAssetService = visualAssetService;
  }

  /** Lista o kit visual versionado do plano comercial. */
  @GetMapping("/{id}/visual-assets")
  public List<CommercialPlanVisualAssetDto> listVisualAssets(@PathVariable Long id) {
    return visualAssetService.list(id);
  }

  /** Anexa uma referência ao kit visual como rascunho auditável. */
  @PostMapping("/{id}/visual-assets")
  public CommercialPlanVisualAssetDto createVisualAsset(
      @PathVariable Long id, @RequestBody CreateCommercialPlanVisualAssetRequest request) {
    return visualAssetService.create(id, request);
  }

  /** Aprova ou retira uma referência visual sem apagar seu histórico. */
  @PatchMapping("/{id}/visual-assets/{assetId}/status")
  public CommercialPlanVisualAssetDto updateVisualAssetStatus(
      @PathVariable Long id,
      @PathVariable Long assetId,
      @RequestBody UpdateCommercialPlanVisualAssetStatusRequest request) {
    return visualAssetService.updateStatus(id, assetId, request.status());
  }

  /** Cria um plano comercial de primeira venda. */
  @PostMapping
  public CommercialPlanDto create(@RequestBody CreateCommercialPlanRequest request) {
    var plan = service.create(request);
    return mapper.toDto(
        plan, service.listMilestones(plan.getId()), service.listSimulations(plan.getId()));
  }

  /** Lista planos comerciais com filtro opcional por status. */
  @GetMapping
  public List<CommercialPlanDto> list(@RequestParam(required = false) CommercialPlanStatus status) {
    return service.list(status).stream()
        .map(
            plan ->
                mapper.toDto(
                    plan,
                    service.listMilestones(plan.getId()),
                    service.listSimulations(plan.getId())))
        .toList();
  }

  /** Busca o detalhe de um plano comercial. */
  @GetMapping("/{id}")
  public CommercialPlanDto get(@PathVariable Long id) {
    return mapper.toDto(
        service.getPlan(id), service.listMilestones(id), service.listSimulations(id));
  }

  /** Lista as versões imutáveis que orientaram usuários, agentes e gates. */
  @GetMapping("/{id}/versions")
  public List<CommercialPlanVersionDto> listVersions(@PathVariable Long id) {
    service.getPlan(id);
    return versionService.list(id);
  }

  /** Entrega aos agentes o contexto comercial corrente com identidade de versão. */
  @GetMapping("/{id}/context/current")
  public CommercialPlanVersionDto currentContext(@PathVariable Long id) {
    service.getPlan(id);
    return versionService.current(id);
  }

  /** Exibe trabalhos, gates, dificuldades e finanças dos agentes vinculados ao plano. */
  @GetMapping("/{id}/agent-activity")
  public CommercialPlanAgentActivityDto agentActivity(@PathVariable Long id) {
    return agentActivityService.activity(service.getPlan(id));
  }

  /** Exibe o fluxo comercial simplificado e a única próxima ação canônica. */
  @GetMapping("/{id}/operational-flow")
  public CommercialPlanOperationalFlowDto operationalFlow(@PathVariable Long id) {
    return operationalFlowService.view(service.getPlan(id));
  }

  /** Adiciona um experimento ao portfólio de testes do plano. */
  @PostMapping("/{id}/experiments/{experimentId}")
  public CommercialPlanDto addExperiment(@PathVariable Long id, @PathVariable Long experimentId) {
    var plan = service.addExperiment(id, experimentId);
    return mapper.toDto(plan, service.listMilestones(id), service.listSimulations(id));
  }

  /** Solicita a homologação integral da jornada de um experimento escolhido do plano. */
  @PostMapping("/{id}/journey-homologations")
  public CommercialPlanJourneyHomologationDto requestJourneyHomologation(
      @PathVariable Long id, @RequestParam Long experimentId) {
    return journeyHomologationService.request(id, experimentId);
  }

  /**
   * Lista os experimentos de cada semana do mes de referencia e os objetivos planejados para a
   * semana seguinte.
   */
  @GetMapping("/{id}/weeks")
  public List<CommercialPlanWeekDto> listWeeks(
      @PathVariable Long id, @RequestParam(required = false) String referenceMonth) {
    return weeklyExperimentService.listWeeks(id, referenceMonth);
  }

  /** Atualiza os objetivos planejados para a semana seguinte ao card informado. */
  @PutMapping("/{id}/weeks/{weekNumber}/objectives")
  public List<CommercialPlanWeekObjectiveDto> updateWeekObjectives(
      @PathVariable Long id,
      @PathVariable Integer weekNumber,
      @RequestBody UpdateCommercialPlanWeekObjectivesRequest request) {
    return weeklyExperimentService.updateObjectives(id, weekNumber, request);
  }

  /** Registra o andamento de um compromisso semanal sem alterar a estratégia congelada. */
  @PatchMapping("/{id}/weeks/commitments/{commitmentId}/status")
  public CommercialPlanWeekObjectiveDto updateWeekCommitmentStatus(
      @PathVariable Long id,
      @PathVariable Long commitmentId,
      @RequestBody UpdateCommercialPlanWeekCommitmentStatusRequest request) {
    return weeklyExperimentService.updateCommitmentStatus(id, commitmentId, request);
  }

  /** Atualiza um plano comercial existente. */
  @PutMapping("/{id}")
  public CommercialPlanDto update(
      @PathVariable Long id, @RequestBody UpdateCommercialPlanRequest request) {
    var plan = service.update(id, request);
    return mapper.toDto(plan, service.listMilestones(id), service.listSimulations(id));
  }

  /** Atualiza um marco comercial do plano. */
  @PatchMapping("/{planId}/milestones/{milestoneId}")
  public CommercialPlanMilestoneDto updateMilestone(
      @PathVariable Long planId,
      @PathVariable Long milestoneId,
      @RequestBody UpdateCommercialPlanMilestoneRequest request) {
    return mapper.toMilestoneDto(service.updateMilestone(planId, milestoneId, request));
  }

  /** Gera uma simulacao manual assistida para o plano. */
  @PostMapping("/{planId}/simulations")
  public CommercialPlanSimulationDto simulate(
      @PathVariable Long planId, @RequestBody CreateCommercialPlanSimulationRequest request) {
    return mapper.toSimulationDto(service.simulate(planId, request));
  }
}
