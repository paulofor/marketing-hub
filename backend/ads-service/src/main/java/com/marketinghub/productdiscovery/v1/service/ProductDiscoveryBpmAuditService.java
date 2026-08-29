package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTaskBlockerGuidanceRequest;
import com.marketinghub.agenttask.AgentTaskExecutionAuditRequest;
import com.marketinghub.agenttask.AgentTaskHelpLinkRequest;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.agenttask.FailAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Responsabilidade: correlacionar o ciclo técnico de descoberta com sua execução auditável no BPM.
 */
@Service
public class ProductDiscoveryBpmAuditService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ProductDiscoveryBpmAuditService.class);
  private static final String PROCESS_CODE = "pde-opportunity-discovery";
  private static final String PRIMARY_ACTIVITY_ID = "marketEvidence";
  private static final String LEGACY_ACTIVITY_ID = "inspiration";
  private static final String OLDER_LEGACY_ACTIVITY_ID = "evidence";
  private static final String AGENT_KEY = "market-radar";
  private static final String EXECUTION_SOURCE_PREFIX = "product-discovery-cycle:";
  private final BusinessProcessDefinitionRepository processRepository;
  private final AgentTaskService agentTaskService;
  private final ObjectMapper objectMapper;

  /** Inicializa a correlação com o catálogo publicado e a mesa canônica dos agentes. */
  public ProductDiscoveryBpmAuditService(
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskService agentTaskService,
      ObjectMapper objectMapper) {
    this.processRepository = processRepository;
    this.agentTaskService = agentTaskService;
    this.objectMapper = objectMapper;
  }

  /** Abre idempotentemente a tarefa BPM que representa um ciclo novo de pesquisa PDE. */
  public AgentTaskResponse open(ProductDiscoveryCycle cycle) {
    requirePersistedCycle(cycle);
    BusinessProcessDefinition process = publishedProcess();
    String activityId = resolveInitialActivityId(process);
    return agentTaskService.createByHumanIfAbsentAcrossProcessVersions(
        new CreateAgentTaskRequest(
            AGENT_KEY,
            "Marketing Hub",
            executionTitle(cycle),
            executionDescription(cycle),
            "HIGH",
            sourceReference(cycle),
            process.getId(),
            activityId,
            false,
            null),
        List.of(PRIMARY_ACTIVITY_ID, LEGACY_ACTIVITY_ID, OLDER_LEGACY_ACTIVITY_ID));
  }

  /** Marca o recebimento real da execução quando o backend entrega o ciclo ao worker. */
  public void start(ProductDiscoveryCycle cycle) {
    AgentTaskResponse task = open(cycle);
    agentTaskService.claimLinkedProcessTask(AGENT_KEY, task.id());
  }

  /** Registra modelo, prompt e tokens disponíveis assim que Argos persiste o plano de pesquisa. */
  public void recordPlan(ProductDiscoveryCycle cycle, ProductDiscoveryResearchPlanRequest request) {
    Long taskId = ensureClaimed(cycle);
    agentTaskService.recordClaimedProcessTaskExecutionAudit(
        AGENT_KEY,
        taskId,
        request.model(),
        request.reasoningEffort(),
        request.promptSent(),
        request.inputTokens(),
        request.cachedInputTokens(),
        request.outputTokens(),
        modelInvocation(request.executionMode()));
  }

  /** Conclui a tarefa BPM com decisão, oportunidades e plano estruturados do ciclo real. */
  public void complete(
      ProductDiscoveryCycle cycle, List<ProductDiscoveryOpportunity> opportunities) {
    Long taskId = ensureClaimed(cycle);
    agentTaskService.completeClaimedProcessTask(
        AGENT_KEY,
        taskId,
        new CompleteAgentTaskRequest(
            completedResult(cycle, opportunities).toString(), evidence(cycle).toString()));
  }

  /** Bloqueia a tarefa BPM preservando a auditoria disponível da tentativa do modelo. */
  public void fail(
      ProductDiscoveryCycle cycle, AgentTaskExecutionAuditRequest failedExecutionAudit) {
    Long taskId = ensureClaimed(cycle);
    agentTaskService.failClaimedProcessTask(
        AGENT_KEY,
        taskId,
        new FailAgentTaskRequest(
            cycle.getErrorMessage(),
            failedResult(cycle).toString(),
            evidence(cycle).toString(),
            null,
            failedExecutionAudit != null ? failedExecutionAudit : prePlanAudit(cycle),
            new AgentTaskBlockerGuidanceRequest(
                "TECHNICAL_FAILURE",
                "Corrija a causa registrada no ciclo de descoberta e reinicie a tarefa de Argos.",
                List.of(
                    new AgentTaskHelpLinkRequest("Abrir tarefas dos agentes", "/agent-tasks")))));
  }

  /** Declara explicitamente quando a falha ocorreu antes de existir um plano executado. */
  private AgentTaskExecutionAuditRequest prePlanAudit(ProductDiscoveryCycle cycle) {
    if (StringUtils.hasText(cycle.getResearchPlanModel())) return null;
    return new AgentTaskExecutionAuditRequest("NOT_STARTED", null, null, null, List.of());
  }

  /** Garante que callbacks diretos ou ciclos anteriores também possuam uma tarefa reservada. */
  private Long ensureClaimed(ProductDiscoveryCycle cycle) {
    AgentTaskResponse task = open(cycle);
    agentTaskService.claimLinkedProcessTask(AGENT_KEY, task.id());
    return task.id();
  }

  /** Localiza a versão publicada que era vigente quando o ciclo foi aberto. */
  private BusinessProcessDefinition publishedProcess() {
    return processRepository
        .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(PROCESS_CODE, "PUBLISHED")
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Processo publicado de descoberta PDE não encontrado para auditoria."));
  }

  /**
   * Prefere a qualificação de fontes da versão atual e mantém compatibilidade com versões antigas.
   */
  private String resolveInitialActivityId(BusinessProcessDefinition process) {
    try {
      JsonNode nodes = objectMapper.readTree(process.getDiagramJson()).path("nodes");
      for (String candidate :
          List.of(PRIMARY_ACTIVITY_ID, LEGACY_ACTIVITY_ID, OLDER_LEGACY_ACTIVITY_ID)) {
        for (JsonNode node : nodes) {
          if (candidate.equals(node.path("id").asText())
              && "TASK".equals(node.path("type").asText())) {
            return candidate;
          }
        }
      }
      throw new IllegalStateException(
          "Processo publicado de descoberta PDE não possui atividade inicial compatível.");
    } catch (IllegalStateException ex) {
      throw ex;
    } catch (Exception ex) {
      LOGGER.error(
          "Falha ao resolver atividade inicial da descoberta PDE. processDefinitionId={}",
          process.getId(),
          ex);
      throw new IllegalStateException(
          "Não foi possível interpretar o processo publicado de descoberta PDE.", ex);
    }
  }

  /** Monta o resultado terminal sem duplicar payload técnico dentro de campos textuais. */
  private ObjectNode completedResult(
      ProductDiscoveryCycle cycle, List<ProductDiscoveryOpportunity> opportunities) {
    ObjectNode result = baseResult(cycle);
    result.put("decisionSummary", cycle.getDecisionSummary());
    ArrayNode rankedOpportunities = result.putArray("opportunities");
    opportunities.forEach(
        opportunity -> {
          ObjectNode item = rankedOpportunities.addObject();
          item.put("name", opportunity.getName());
          item.put("decision", opportunity.getDecision().name());
          item.put("score", opportunity.getScore());
        });
    return result;
  }

  /** Monta o resultado bloqueado preservando a etapa em que o callback falhou. */
  private ObjectNode failedResult(ProductDiscoveryCycle cycle) {
    ObjectNode result = baseResult(cycle);
    result.put("error", cycle.getErrorMessage());
    return result;
  }

  /** Cria a identidade funcional mínima compartilhada pelos resultados terminais. */
  private ObjectNode baseResult(ProductDiscoveryCycle cycle) {
    ObjectNode result = objectMapper.createObjectNode();
    result.put("cycleId", cycle.getId());
    result.put("status", cycle.getStatus().name());
    result.put("stageCode", cycle.getStageCode());
    return result;
  }

  /** Preserva plano, resposta bruta e tentativa como estruturas JSON auditáveis. */
  private ObjectNode evidence(ProductDiscoveryCycle cycle) {
    ObjectNode evidence = objectMapper.createObjectNode();
    evidence.put("cycleId", cycle.getId());
    evidence.put("sourceReference", sourceReference(cycle));
    evidence.put("executionAttempt", cycle.getExecutionAttempt());
    if (StringUtils.hasText(cycle.getResearchPlanModel())) {
      evidence.put("researchPlanModel", cycle.getResearchPlanModel());
    }
    if (StringUtils.hasText(cycle.getResearchPlanJson())) {
      evidence.set(
          "researchPlan", parseAuditJson(cycle, "researchPlan", cycle.getResearchPlanJson()));
    }
    if (StringUtils.hasText(cycle.getResearchPlanRawResponse())) {
      evidence.set(
          "researchPlanRawResponse",
          parseAuditJson(cycle, "researchPlanRawResponse", cycle.getResearchPlanRawResponse()));
    }
    return evidence;
  }

  /** Interpreta JSON auditável e bloqueia conteúdo corrompido antes de concluir a tarefa. */
  private JsonNode parseAuditJson(ProductDiscoveryCycle cycle, String field, String value) {
    try {
      JsonNode parsed = objectMapper.readTree(value);
      if (parsed == null) throw new IllegalArgumentException("JSON vazio");
      return parsed;
    } catch (Exception ex) {
      LOGGER.error(
          "Falha ao estruturar auditoria da descoberta PDE. cycleId={} campo={}",
          cycle.getId(),
          field,
          ex);
      throw new IllegalStateException("Auditoria JSON do ciclo de descoberta está inválida.", ex);
    }
  }

  /** Distingue chamada real de modelo do fallback determinístico sem inferir modo legado. */
  private Boolean modelInvocation(String executionMode) {
    if (!StringUtils.hasText(executionMode)) return null;
    if ("CODEX".equalsIgnoreCase(executionMode.trim())) return true;
    if ("DETERMINISTIC".equalsIgnoreCase(executionMode.trim())) return false;
    throw new IllegalArgumentException("Modo de execução do plano de pesquisa não reconhecido.");
  }

  /** Garante identidade persistida antes de criar a referência BPM do ciclo. */
  private void requirePersistedCycle(ProductDiscoveryCycle cycle) {
    if (cycle == null || cycle.getId() == null) {
      throw new IllegalArgumentException("Auditoria BPM exige ciclo de descoberta persistido.");
    }
  }

  /** Produz a referência estável que correlaciona ciclo, tarefa e ocorrência BPM. */
  private String sourceReference(ProductDiscoveryCycle cycle) {
    return EXECUTION_SOURCE_PREFIX + cycle.getId();
  }

  /** Mantém o título operacional dentro do limite persistido pela caixa de entrada. */
  private String executionTitle(ProductDiscoveryCycle cycle) {
    String prefix = "Pesquisar oportunidade PDE #" + cycle.getId() + " · ";
    String theme =
        StringUtils.hasText(cycle.getTheme()) ? cycle.getTheme().trim() : "tema informado";
    int available = Math.max(0, 160 - prefix.length());
    return prefix + theme.substring(0, Math.min(theme.length(), available));
  }

  /** Descreve o objetivo real do ciclo sem criar promessa ou decisão ainda inexistente. */
  private String executionDescription(ProductDiscoveryCycle cycle) {
    if (StringUtils.hasText(cycle.getObjective())) return cycle.getObjective().trim();
    return "Qualificar fontes e pesquisar evidências auditáveis para " + cycle.getTheme() + ".";
  }
}
