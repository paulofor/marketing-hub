package com.marketinghub.planning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.agent.v1.LandingGenerationAgentExecutionService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanJourneyHomologationDto;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: solicitar a Dédalo a homologação técnica da jornada vinculada ao plano. */
@Service
public class CommercialPlanJourneyHomologationService {
  private final CommercialPlanService commercialPlanService;
  private final LandingGenerationAgentExecutionService executionService;
  private final ObjectMapper objectMapper;

  /** Inicializa o serviço com as fontes canônicas do plano e da fila de Dédalo. */
  public CommercialPlanJourneyHomologationService(
      CommercialPlanService commercialPlanService,
      LandingGenerationAgentExecutionService executionService,
      ObjectMapper objectMapper) {
    this.commercialPlanService = commercialPlanService;
    this.executionService = executionService;
    this.objectMapper = objectMapper;
  }

  /** Enfileira uma auditoria integral segregada, sem publicar landing ou liberar mídia. */
  @Transactional
  public CommercialPlanJourneyHomologationDto request(Long planId, Long experimentId) {
    commercialPlanService.requireExperiment(planId, experimentId);
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    Instant requestedAt = Instant.now();
    String cycleId = "cph-" + planId + "-" + UUID.randomUUID().toString().substring(0, 20);
    executionService.enqueue(experimentId, cycleId, buildAuditBrief(plan, experimentId));
    return new CommercialPlanJourneyHomologationDto(
        planId, experimentId, "INICIADO", requestedAt.toString());
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
