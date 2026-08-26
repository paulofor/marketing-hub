package com.marketinghub.planning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanJourneyHomologationDto;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: iniciar a homologação oficial da landing vinculada ao plano comercial. */
@Service
public class CommercialPlanJourneyHomologationService {
  private static final String LANDING_PROCESS_CODE = "landing-page-generation";
  private final CommercialPlanService commercialPlanService;
  private final CommercialPlanVersionService versionService;
  private final BusinessProcessDefinitionRepository processRepository;
  private final AgentTaskService agentTaskService;
  private final ObjectMapper objectMapper;

  /** Inicializa o serviço com o plano, sua versão e o processo BPM publicado. */
  public CommercialPlanJourneyHomologationService(
      CommercialPlanService commercialPlanService,
      CommercialPlanVersionService versionService,
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskService agentTaskService,
      ObjectMapper objectMapper) {
    this.commercialPlanService = commercialPlanService;
    this.versionService = versionService;
    this.processRepository = processRepository;
    this.agentTaskService = agentTaskService;
    this.objectMapper = objectMapper;
  }

  /** Abre Dédalo, Psique e Têmis na mesma instância BPM, sem publicar landing ou liberar mídia. */
  @Transactional
  public CommercialPlanJourneyHomologationDto request(Long planId, Long experimentId) {
    commercialPlanService.requireExperiment(planId, experimentId);
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    Instant requestedAt = Instant.now();
    BusinessProcessDefinition process = publishedLandingProcess();
    int planVersion = versionService.current(planId).versionNumber();
    String sourceReference = "commercial-plan:" + planId + "@v" + planVersion + ":journey";
    String auditBrief = buildAuditBrief(plan, experimentId);
    createTask(
        "landing-generator",
        "html",
        "Experimento #" + experimentId + " · construir e homologar a landing",
        auditBrief,
        sourceReference,
        process);
    createTask(
        "customer-agent",
        "customer",
        "Experimento #" + experimentId + " · validar percepção da cliente",
        "Avaliar a landing final aprovada pelo Quality Review e bloquear qualquer divergência entre promessa, entrega e checkout.",
        sourceReference,
        process);
    createTask(
        "meta-ad-approver",
        "commercial",
        "Experimento #" + experimentId + " · revisar coerência comercial da landing",
        "Revisar a mesma landing final depois de Psique e bloquear divergência de oferta, preço, prova, CTA, checkout ou instrumentação.",
        sourceReference,
        process);
    return new CommercialPlanJourneyHomologationDto(
        planId, experimentId, "INICIADO", requestedAt.toString());
  }

  /** Localiza somente a versão publicada do subprocesso oficial de landing. */
  private BusinessProcessDefinition publishedLandingProcess() {
    return processRepository
        .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(LANDING_PROCESS_CODE, "PUBLISHED")
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O subprocesso oficial de landing ainda não está publicado."));
  }

  /** Cria cada gate uma única vez para a versão corrente do plano. */
  private void createTask(
      String agentKey,
      String activityId,
      String title,
      String description,
      String sourceReference,
      BusinessProcessDefinition process) {
    agentTaskService.createByHumanIfAbsent(
        new CreateAgentTaskRequest(
            agentKey,
            "Operador do Marketing Hub",
            title,
            description,
            "HIGH",
            sourceReference,
            process.getId(),
            activityId,
            false,
            null));
  }

  /** Monta o contexto estruturado que restringe a execução à homologação sem tráfego real. */
  private String buildAuditBrief(CommercialPlan plan, Long experimentId) {
    Map<String, Object> brief = new LinkedHashMap<>();
    brief.put("approvalRecommendation", "REGENERATE_BEFORE_PUBLICATION");
    brief.put("score", 0);
    brief.put("source", "COMMERCIAL_PLAN_JOURNEY_HOMOLOGATION");
    brief.put("commercialPlanId", plan.getId());
    brief.put("experimentId", experimentId);
    brief.put("successCriteria", plan.getSuccessCriteria());
    brief.put("stopCriteria", plan.getStopCriteria());
    brief.put("currentBlocker", plan.getCurrentBlocker());
    brief.put("rootCause", plan.getRootCause());
    brief.put("nextAction", plan.getNextAction());
    brief.put("testIsolation", "mh_test=1");
    brief.put("recoveryPolicy", "BPM_TASK_RETRY_WITH_PERSISTED_CAUSE");
    brief.put("publicationAuthorized", false);
    brief.put("mediaSpendAuthorized", false);
    brief.put(
        "requiredEvidence",
        List.of(
            "landing e eventos sem duplicidade",
            "amostra personalizada",
            "e-mail de entrega",
            "checkout em modo de teste",
            "pagamento de teste",
            "briefing",
            "produção",
            "entrega final"));
    brief.put(
        "objective",
        "Homologar integralmente a jornada com dados de teste segregados, corrigir defeitos técnicos e registrar evidências auditáveis.");
    try {
      return objectMapper.writeValueAsString(brief);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Não foi possível montar o contexto da homologação", ex);
    }
  }
}
