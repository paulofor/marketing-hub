package com.marketinghub.agentorchestration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agentorchestration.AgentOrchestrationCase;
import com.marketinghub.agentorchestration.AgentOrchestrationStatus;
import com.marketinghub.agentorchestration.AgentTaskState;
import com.marketinghub.creative.Creative;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.growthoperator.GrowthOperatorExecution;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.agentorchestration.AgentOrchestrationCaseRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import com.marketinghub.repository.jpa.growthoperator.GrowthOperatorExecutionRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: reconciliar gates persistidos dos agentes sem executar suas recomendacoes. */
@Service
public class AgentOrchestrationService {
  private final AgentOrchestrationCaseRepository cases;
  private final CommercialPlanService plans;
  private final ExperimentStrategistExecutionRepository strategistExecutions;
  private final GrowthOperatorExecutionRepository growthExecutions;
  private final CreativeRepository creatives;
  private final ObjectMapper json;

  /** Configura as fontes de verdade usadas na coordenacao deterministica. */
  public AgentOrchestrationService(
      AgentOrchestrationCaseRepository cases,
      CommercialPlanService plans,
      ExperimentStrategistExecutionRepository strategistExecutions,
      GrowthOperatorExecutionRepository growthExecutions,
      CreativeRepository creatives,
      ObjectMapper json) {
    this.cases = cases;
    this.plans = plans;
    this.strategistExecutions = strategistExecutions;
    this.growthExecutions = growthExecutions;
    this.creatives = creatives;
    this.json = json;
  }

  /** Cria ou reconcilia o caso do experimento vinculado sem duplicar trabalho. */
  @Transactional
  public OrchestrationResponse synchronize(Long planId) {
    CommercialPlan plan = plans.getPlan(planId);
    if (plan.getExperiment() == null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Planejamento sem experimento para coordenar.");
    }
    Long experimentId = plan.getExperiment().getId();
    AgentOrchestrationCase value =
        cases
            .findByCommercialPlanIdAndExperimentId(planId, experimentId)
            .orElseGet(AgentOrchestrationCase::new);
    value.setCommercialPlan(plan);
    value.setExperimentId(experimentId);

    ExperimentStrategistExecution strategist =
        strategistExecutions.findFirstByCommercialPlanIdOrderByCreatedAtDesc(planId).orElse(null);
    GrowthOperatorExecution operator =
        growthExecutions.findFirstByCommercialPlanIdOrderByCreatedAtDesc(planId).orElse(null);
    Creative creative = creatives.findFirstByExperimentIdOrderByIdDesc(experimentId).orElse(null);

    value.setStrategistExecutionId(strategist == null ? null : strategist.getId());
    value.setGrowthOperatorExecutionId(operator == null ? null : operator.getId());
    value.setCreativeId(creative == null ? null : creative.getId());
    value.setStrategistState(strategistState(strategist));
    value.setGrowthOperatorState(operatorState(operator, experimentId));
    value.setAdSpecialistState(adState(creative));
    value.setStatus(overallStatus(value));
    value.setBlocker(blocker(value, operator, experimentId));
    value.setEvidenceSnapshot(
        snapshot(planId, experimentId, value, strategist, operator, creative));
    return response(cases.save(value));
  }

  /** Lista os casos auditaveis sem provocar nova execucao ou mutacao comercial. */
  @Transactional(readOnly = true)
  public List<OrchestrationResponse> list(Long planId) {
    plans.getPlan(planId);
    return cases.findByCommercialPlanIdOrderByUpdatedAtDesc(planId).stream()
        .map(this::response)
        .toList();
  }

  /** Normaliza o estado do parecer do Estrategista. */
  private AgentTaskState strategistState(ExperimentStrategistExecution execution) {
    if (execution == null) return AgentTaskState.REQUIRED;
    return switch (execution.getStatus()) {
      case PENDING, RUNNING -> AgentTaskState.IN_PROGRESS;
      case COMPLETED -> AgentTaskState.COMPLETED;
      case FAILED -> AgentTaskState.BLOCKED;
    };
  }

  /** Normaliza o estado do Operador e bloqueia contexto de experimento divergente. */
  private AgentTaskState operatorState(GrowthOperatorExecution execution, Long experimentId) {
    if (execution == null) return AgentTaskState.REQUIRED;
    if (!operatorMatchesExperiment(execution, experimentId)) {
      return AgentTaskState.BLOCKED;
    }
    return switch (execution.getStatus()) {
      case PENDING, RUNNING -> AgentTaskState.IN_PROGRESS;
      case COMPLETED -> AgentTaskState.COMPLETED;
      case FAILED -> AgentTaskState.BLOCKED;
    };
  }

  /** Normaliza o gate do Especialista em Anuncios. */
  private AgentTaskState adState(Creative creative) {
    if (creative == null || creative.getAgentReviewStatus() == null) return AgentTaskState.REQUIRED;
    return switch (creative.getAgentReviewStatus()) {
      case PENDING, PROCESSING -> AgentTaskState.IN_PROGRESS;
      case APPROVED -> AgentTaskState.COMPLETED;
      case ADJUST, REJECTED, FAILED -> AgentTaskState.BLOCKED;
    };
  }

  /** Calcula o gate consolidado sem autorizar gasto ou publicacao. */
  private AgentOrchestrationStatus overallStatus(AgentOrchestrationCase value) {
    if (value.getStrategistState() == AgentTaskState.BLOCKED
        || value.getGrowthOperatorState() == AgentTaskState.BLOCKED
        || value.getAdSpecialistState() == AgentTaskState.BLOCKED) {
      return AgentOrchestrationStatus.BLOCKED;
    }
    if (value.getStrategistState() == AgentTaskState.COMPLETED
        && value.getGrowthOperatorState() == AgentTaskState.COMPLETED
        && value.getAdSpecialistState() == AgentTaskState.COMPLETED) {
      return AgentOrchestrationStatus.READY_FOR_HUMAN_DECISION;
    }
    return AgentOrchestrationStatus.WAITING_FOR_AGENTS;
  }

  /** Explica a primeira dependencia impeditiva em linguagem operacional. */
  private String blocker(
      AgentOrchestrationCase value, GrowthOperatorExecution operator, Long experimentId) {
    if (value.getGrowthOperatorState() == AgentTaskState.BLOCKED
        && operator != null
        && !operatorMatchesExperiment(operator, experimentId)) {
      return "Operador vinculado a experimento diferente do caso.";
    }
    if (value.getStrategistState() == AgentTaskState.BLOCKED)
      return "Parecer do Estrategista falhou e precisa ser refeito.";
    if (value.getGrowthOperatorState() == AgentTaskState.BLOCKED)
      return "Diagnostico do Operador falhou e precisa ser refeito.";
    if (value.getAdSpecialistState() == AgentTaskState.BLOCKED)
      return "Criativo nao passou pelo gate do Especialista em Anuncios.";
    if (value.getStrategistState() == AgentTaskState.REQUIRED)
      return "Parecer comparativo do Estrategista ainda nao solicitado.";
    if (value.getGrowthOperatorState() == AgentTaskState.REQUIRED)
      return "Diagnostico do Operador ainda nao solicitado.";
    if (value.getAdSpecialistState() == AgentTaskState.REQUIRED)
      return "Criativo ainda nao submetido ao Especialista em Anuncios.";
    return null;
  }

  /** Confirma no snapshot historico que o Operador recebeu o experimento do caso. */
  private boolean operatorMatchesExperiment(GrowthOperatorExecution execution, Long experimentId) {
    if (execution.getEvidenceSnapshot() == null || execution.getEvidenceSnapshot().isBlank()) {
      return false;
    }
    try {
      JsonNode root = json.readTree(execution.getEvidenceSnapshot());
      JsonNode direct = root.path("experimentId");
      JsonNode nested = root.path("experiment").path("id");
      long frozenId = direct.isNumber() ? direct.asLong() : nested.asLong(-1L);
      return experimentId.equals(frozenId);
    } catch (JsonProcessingException ex) {
      return false;
    }
  }

  /** Congela as referencias verificadas na reconciliacao para auditoria. */
  private String snapshot(
      Long planId,
      Long experimentId,
      AgentOrchestrationCase value,
      ExperimentStrategistExecution strategist,
      GrowthOperatorExecution operator,
      Creative creative) {
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("commercialPlanId", planId);
    evidence.put("experimentId", experimentId);
    evidence.put("strategistExecutionId", strategist == null ? null : strategist.getId());
    evidence.put("strategistState", value.getStrategistState());
    evidence.put("growthOperatorExecutionId", operator == null ? null : operator.getId());
    evidence.put("growthOperatorState", value.getGrowthOperatorState());
    evidence.put("creativeId", creative == null ? null : creative.getId());
    evidence.put("adSpecialistState", value.getAdSpecialistState());
    evidence.put("humanApprovalRequired", true);
    evidence.put("synchronizedAt", Instant.now().toString());
    try {
      return json.writeValueAsString(evidence);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Falha ao registrar evidencias da orquestracao.", ex);
    }
  }

  /** Converte o caso persistido no contrato publico. */
  private OrchestrationResponse response(AgentOrchestrationCase value) {
    return new OrchestrationResponse(
        value.getId(),
        value.getCommercialPlan().getId(),
        value.getExperimentId(),
        value.getStatus(),
        value.getStrategistState(),
        value.getGrowthOperatorState(),
        value.getAdSpecialistState(),
        value.getStrategistExecutionId(),
        value.getGrowthOperatorExecutionId(),
        value.getCreativeId(),
        value.getBlocker(),
        value.getEvidenceSnapshot(),
        value.getCreatedAt(),
        value.getUpdatedAt(),
        true);
  }

  /** Contrato de leitura do caso coordenado. */
  public record OrchestrationResponse(
      Long id,
      Long commercialPlanId,
      Long experimentId,
      AgentOrchestrationStatus status,
      AgentTaskState strategistState,
      AgentTaskState growthOperatorState,
      AgentTaskState adSpecialistState,
      Long strategistExecutionId,
      Long growthOperatorExecutionId,
      Long creativeId,
      String blocker,
      String evidenceSnapshot,
      Instant createdAt,
      Instant updatedAt,
      boolean humanApprovalRequired) {}
}
