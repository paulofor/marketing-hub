package com.marketinghub.businessprocess.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionHistoryResponse;
import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionResponse;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: consultar as tarefas recentes de uma atividade BPM com auditoria completa. */
@Service
public class BusinessProcessActivityExecutionService {
  private static final int RECENT_EXECUTION_LIMIT = 10;
  private static final Pattern COMMERCIAL_PLAN_REFERENCE =
      Pattern.compile("^commercial-plan:(\\d+)@.*$");
  private static final Logger LOGGER =
      LoggerFactory.getLogger(BusinessProcessActivityExecutionService.class);

  private final BusinessProcessDefinitionRepository processRepository;
  private final AgentTaskRepository taskRepository;
  private final CommercialPlanRepository commercialPlanRepository;
  private final GeraLandingStageExecutionRepository landingExecutionRepository;
  private final ObjectMapper objectMapper;

  /** Configura as fontes canônicas do processo, das tarefas e do produto. */
  @Autowired
  public BusinessProcessActivityExecutionService(
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskRepository taskRepository,
      CommercialPlanRepository commercialPlanRepository,
      GeraLandingStageExecutionRepository landingExecutionRepository,
      ObjectMapper objectMapper) {
    this.processRepository = processRepository;
    this.taskRepository = taskRepository;
    this.commercialPlanRepository = commercialPlanRepository;
    this.landingExecutionRepository = landingExecutionRepository;
    this.objectMapper = objectMapper;
  }

  /** Permite testes unitários sem carregar o catálogo comercial. */
  BusinessProcessActivityExecutionService(
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskRepository taskRepository,
      ObjectMapper objectMapper) {
    this(processRepository, taskRepository, null, null, objectMapper);
  }

  /** Permite comprovar a projeção da auditoria técnica na tarefa composta. */
  BusinessProcessActivityExecutionService(
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskRepository taskRepository,
      GeraLandingStageExecutionRepository landingExecutionRepository,
      ObjectMapper objectMapper) {
    this(processRepository, taskRepository, null, landingExecutionRepository, objectMapper);
  }

  /** Retorna as dez tarefas mais recentes da atividade em todas as versões do processo canônico. */
  @Transactional(readOnly = true)
  public BusinessProcessActivityExecutionHistoryResponse recentExecutions(
      Long processDefinitionId, String activityId) {
    BusinessProcessDefinition selectedProcess = requiredProcess(processDefinitionId);
    JsonNode activity = requireTaskActivity(selectedProcess, activityId);
    String normalizedActivityId = activity.path("id").asText();
    List<BusinessProcessActivityExecutionResponse> executions =
        taskRepository
            .findRecentActivityExecutions(
                selectedProcess.getProcessCode(),
                normalizedActivityId,
                PageRequest.of(0, RECENT_EXECUTION_LIMIT))
            .stream()
            .limit(RECENT_EXECUTION_LIMIT)
            .map(this::response)
            .toList();
    return new BusinessProcessActivityExecutionHistoryResponse(
        selectedProcess.getId(),
        selectedProcess.getProcessCode(),
        selectedProcess.getName(),
        selectedProcess.getVersionNumber(),
        selectedProcess.getStatus(),
        normalizedActivityId,
        activity.path("label").asText(normalizedActivityId),
        textOrNull(activity.path("owner")),
        executions);
  }

  /** Exige uma definição existente antes de consultar seu histórico operacional. */
  private BusinessProcessDefinition requiredProcess(Long processDefinitionId) {
    return processRepository
        .findById(processDefinitionId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processo não encontrado."));
  }

  /** Confirma que o identificador pertence a uma atividade executável da definição selecionada. */
  private JsonNode requireTaskActivity(BusinessProcessDefinition process, String activityId) {
    String normalized = activityId == null ? "" : activityId.trim();
    if (normalized.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Atividade não informada.");
    }
    try {
      JsonNode nodes = objectMapper.readTree(process.getDiagramJson()).path("nodes");
      return StreamSupport.stream(nodes.spliterator(), false)
          .filter(
              node ->
                  normalized.equals(node.path("id").asText())
                      && "TASK".equals(node.path("type").asText()))
          .findFirst()
          .orElseThrow(
              () ->
                  new ResponseStatusException(
                      HttpStatus.NOT_FOUND, "Atividade não encontrada neste processo."));
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      LOGGER.error(
          "Falha ao ler diagrama para consultar execuções BPM. processDefinitionId={} activityId={}",
          process.getId(),
          normalized,
          ex);
      throw new IllegalStateException("Não foi possível ler o diagrama do processo.", ex);
    }
  }

  /** Converte a tarefa em uma execução sem perder versão, estado, custo ou conteúdo auditado. */
  private BusinessProcessActivityExecutionResponse response(AgentTask task) {
    BusinessProcessDefinition process = task.getProcessDefinition();
    Optional<GeraLandingStageExecution> technicalExecution = landingExecution(task);
    return new BusinessProcessActivityExecutionResponse(
        task.getId(),
        process.getId(),
        process.getVersionNumber(),
        task.getTitle(),
        task.getStatus(),
        task.getSourceReference(),
        task.getAssignedAgent().getAgentKey(),
        task.getAssignedAgent().getNickname(),
        technicalExecution
            .map(GeraLandingStageExecution::getModelResponse)
            .filter(value -> !value.isBlank())
            .orElse(task.getResultJson()),
        task.getEvidenceJson(),
        task.getExecutionError(),
        task.getInputTokens(),
        task.getCachedInputTokens(),
        task.getOutputTokens(),
        task.getEstimatedCostUsd(),
        task.getCostEstimationStatus(),
        task.getCreatedAt(),
        technicalExecution
            .map(GeraLandingStageExecution::getProcessingStartedAt)
            .orElse(task.getReceivedAt()),
        technicalExecution.map(GeraLandingStageExecution::getCompletedAt).orElse(finishedAt(task)),
        firstPresent(
            executionModelCode(task),
            technicalExecution.map(GeraLandingStageExecution::getOpenAiModel).orElse(null)),
        executionReasoningEffort(task),
        productInternalName(task),
        firstPresent(
            task.getExecutionPrompt(),
            technicalExecution.map(GeraLandingStageExecution::getPrompt).orElse(null)));
  }

  /** Recupera a chamada real de Dédalo correlacionada à tarefa composta, quando existente. */
  private Optional<GeraLandingStageExecution> landingExecution(AgentTask task) {
    if (landingExecutionRepository == null
        || task.getAssignedAgent() == null
        || !"landing-generator".equals(task.getAssignedAgent().getAgentKey())
        || task.getProcessDefinition() == null
        || !"landing-page-generation".equals(task.getProcessDefinition().getProcessCode())) {
      return Optional.empty();
    }
    return landingExecutionRepository
        .findTop20ByStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
            "landing-generation-agent-v1", "agent-task:" + task.getId())
        .stream()
        .filter(execution -> "CONCLUIDO".equals(execution.getStatus()))
        .findFirst();
  }

  /** Usa o marco entregue ou, para tarefas terminais sem entrega, a última atualização. */
  private Instant finishedAt(AgentTask task) {
    if (task.getDeliveredAt() != null) return task.getDeliveredAt();
    return List.of("BLOCKED", "CANCELLED").contains(task.getStatus()) ? task.getUpdatedAt() : null;
  }

  /** Recupera o modelo das colunas atuais ou das evidências legadas, sem inventar identidade. */
  private String executionModelCode(AgentTask task) {
    return firstPresent(task.getExecutionModelCode(), evidenceText(task, "modelCode", "model"));
  }

  /** Recupera o esforço registrado sem deduzi-lo a partir do modelo. */
  private String executionReasoningEffort(AgentTask task) {
    return firstPresent(
        task.getExecutionReasoningEffort(),
        evidenceText(task, "reasoningEffort", "modelReasoningEffort"));
  }

  /** Lê somente atributos textuais conhecidos do JSON legado de evidência. */
  private String evidenceText(AgentTask task, String... fieldNames) {
    if (task.getEvidenceJson() == null || task.getEvidenceJson().isBlank()) return null;
    try {
      JsonNode evidence = objectMapper.readTree(task.getEvidenceJson());
      for (String fieldName : fieldNames) {
        String value = textOrNull(evidence.path(fieldName));
        if (value != null) return value;
      }
    } catch (Exception ex) {
      LOGGER.debug(
          "Evidência legada não contém JSON para auditoria da tarefa {}.", task.getId(), ex);
    }
    return null;
  }

  /** Converte um nó textual preenchido em valor opcional. */
  private String textOrNull(JsonNode value) {
    return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
  }

  /** Devolve o primeiro texto preenchido e mantém ausente o que não foi registrado. */
  private String firstPresent(String first, String second) {
    if (first != null && !first.isBlank()) return first;
    return second != null && !second.isBlank() ? second : null;
  }

  /** Resolve o produto somente quando a origem aponta para um plano comercial canônico. */
  private String productInternalName(AgentTask task) {
    if (commercialPlanRepository == null || task.getSourceReference() == null) return null;
    Matcher matcher = COMMERCIAL_PLAN_REFERENCE.matcher(task.getSourceReference());
    if (!matcher.matches()) return null;
    try {
      return commercialPlanRepository
          .findById(Long.valueOf(matcher.group(1)))
          .flatMap(this::internalProductName)
          .orElse(null);
    } catch (NumberFormatException ex) {
      LOGGER.warn(
          "Referência comercial inválida ao consultar produto da tarefa {}.", task.getId(), ex);
      return null;
    }
  }

  /** Lê a identidade interna do produto pelos vínculos persistidos do plano. */
  private Optional<String> internalProductName(CommercialPlan plan) {
    if (plan.getHypothesis() != null && plan.getHypothesis().getProduct() != null) {
      return Optional.ofNullable(plan.getHypothesis().getProduct().getInternalName());
    }
    if (plan.getExperiment() != null && plan.getExperiment().getProduct() != null) {
      return Optional.ofNullable(plan.getExperiment().getProduct().getInternalName());
    }
    return plan.getExperiments().stream()
        .map(experiment -> experiment.getProduct())
        .filter(java.util.Objects::nonNull)
        .map(product -> product.getInternalName())
        .filter(value -> value != null && !value.isBlank())
        .findFirst();
  }
}
