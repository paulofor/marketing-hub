package com.marketinghub.businessprocess.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskActivityCoverage;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionHistoryResponse;
import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionResponse;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskActivityCoverageRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

/** Responsabilidade: consultar tarefas BPM auditáveis por atividade, processo e produto. */
@Service
public class BusinessProcessActivityExecutionService {
  private static final int RECENT_EXECUTION_LIMIT = 10;
  private static final String UNLINKED_ACTIVITY_ID = "__unlinked__";
  private static final Pattern COMMERCIAL_PLAN_REFERENCE =
      Pattern.compile("^commercial-plan:(\\d+)@.*$");
  private static final Logger LOGGER =
      LoggerFactory.getLogger(BusinessProcessActivityExecutionService.class);

  private final BusinessProcessDefinitionRepository processRepository;
  private final BusinessProcessActivityDefinitionRepository activityDefinitionRepository;
  private final AgentTaskRepository taskRepository;
  private final AgentTaskActivityCoverageRepository activityCoverageRepository;
  private final CommercialPlanRepository commercialPlanRepository;
  private final GeraLandingStageExecutionRepository landingExecutionRepository;
  private final ProductRepository productRepository;
  private final ObjectMapper objectMapper;

  /** Configura as fontes canônicas do processo, das tarefas, da cobertura e do produto. */
  @Autowired
  public BusinessProcessActivityExecutionService(
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      AgentTaskRepository taskRepository,
      AgentTaskActivityCoverageRepository activityCoverageRepository,
      CommercialPlanRepository commercialPlanRepository,
      GeraLandingStageExecutionRepository landingExecutionRepository,
      ProductRepository productRepository,
      ObjectMapper objectMapper) {
    this.processRepository = processRepository;
    this.activityDefinitionRepository = activityDefinitionRepository;
    this.taskRepository = taskRepository;
    this.activityCoverageRepository = activityCoverageRepository;
    this.commercialPlanRepository = commercialPlanRepository;
    this.landingExecutionRepository = landingExecutionRepository;
    this.productRepository = productRepository;
    this.objectMapper = objectMapper;
  }

  /** Permite testes unitários sem carregar o catálogo comercial. */
  BusinessProcessActivityExecutionService(
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskRepository taskRepository,
      ObjectMapper objectMapper) {
    this(processRepository, null, taskRepository, null, null, null, null, objectMapper);
  }

  /** Permite comprovar a projeção da auditoria técnica na tarefa composta. */
  BusinessProcessActivityExecutionService(
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskRepository taskRepository,
      GeraLandingStageExecutionRepository landingExecutionRepository,
      ObjectMapper objectMapper) {
    this(
        processRepository,
        null,
        taskRepository,
        null,
        null,
        landingExecutionRepository,
        null,
        objectMapper);
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

  /**
   * Consolida as atividades da versão selecionada e todas as tarefas do produto no mesmo processo
   * canônico, preservando atividades históricas e cobertura composta.
   */
  @Transactional(readOnly = true)
  public ProductProcessActivityExecutionHistoryResponse productProcessExecutions(
      Long processDefinitionId, Long productId) {
    BusinessProcessDefinition selectedProcess = requiredProcess(processDefinitionId);
    Product product = requiredProduct(productId);
    List<AgentTask> tasks = productProcessTasks(productId, selectedProcess.getProcessCode());
    List<BusinessProcessActivityDefinition> selectedActivities =
        activityDefinitionRepository.findAllByProcessDefinitionIdOrderByIdAsc(processDefinitionId);

    Map<String, BusinessProcessActivityDefinition> selectedByActivityId = new LinkedHashMap<>();
    Map<String, List<AgentTask>> tasksByActivityId = new LinkedHashMap<>();
    Map<String, String> historicalActivityNames = new LinkedHashMap<>();
    selectedActivities.forEach(
        activity -> {
          selectedByActivityId.put(activity.getActivityId(), activity);
          tasksByActivityId.put(activity.getActivityId(), new java.util.ArrayList<>());
        });

    Map<Long, Set<String>> activityIdsByTask =
        activityIdsByTask(tasks, selectedProcess, historicalActivityNames);
    tasks.forEach(
        task -> {
          Set<String> activityIds = activityIdsByTask.getOrDefault(task.getId(), Set.of());
          if (activityIds.isEmpty()) {
            tasksByActivityId
                .computeIfAbsent(UNLINKED_ACTIVITY_ID, ignored -> new java.util.ArrayList<>())
                .add(task);
            return;
          }
          activityIds.forEach(
              activityId -> {
                tasksByActivityId
                    .computeIfAbsent(activityId, ignored -> new java.util.ArrayList<>())
                    .add(task);
                if (activityId.equals(task.getProcessActivityId())
                    && task.getProcessActivityName() != null
                    && !task.getProcessActivityName().isBlank()) {
                  historicalActivityNames.putIfAbsent(activityId, task.getProcessActivityName());
                }
              });
        });

    Map<Long, BusinessProcessActivityExecutionResponse> taskResponses = new LinkedHashMap<>();
    tasks.forEach(
        task -> taskResponses.put(task.getId(), response(task, product.getInternalName())));
    List<ProductProcessActivityExecutionGroupResponse> activities =
        activityGroups(
            tasksByActivityId, selectedByActivityId, historicalActivityNames, taskResponses);
    BigDecimal knownCost = knownEstimatedCost(tasks);
    return new ProductProcessActivityExecutionHistoryResponse(
        product.getId(),
        product.getName(),
        product.getInternalName(),
        selectedProcess.getId(),
        selectedProcess.getProcessCode(),
        selectedProcess.getName(),
        selectedProcess.getVersionNumber(),
        selectedProcess.getStatus(),
        activities.size(),
        (int) activities.stream().filter(activity -> activity.taskCount() > 0).count(),
        tasks.size(),
        knownCost,
        costCoverage(tasks),
        activities);
  }

  /** Exige um produto existente antes de resolver suas referências operacionais. */
  private Product requiredProduct(Long productId) {
    return productRepository
        .findById(productId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
  }

  /** Busca somente tarefas dos planos do produto e do código estável do processo selecionado. */
  private List<AgentTask> productProcessTasks(Long productId, String processCode) {
    Map<Long, AgentTask> uniqueTasks = new LinkedHashMap<>();
    for (CommercialPlan plan : commercialPlanRepository.findByProductId(productId)) {
      taskRepository
          .findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
              "commercial-plan:" + plan.getId() + "@")
          .stream()
          .filter(task -> task.getProcessDefinition() != null)
          .filter(task -> processCode.equals(task.getProcessDefinition().getProcessCode()))
          .forEach(task -> uniqueTasks.put(task.getId(), task));
    }
    return uniqueTasks.values().stream()
        .sorted(
            Comparator.comparing(
                    AgentTask::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AgentTask::getId, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  /** Reúne o vínculo principal e as atividades adicionais cobertas por cada tarefa composta. */
  private Map<Long, Set<String>> activityIdsByTask(
      List<AgentTask> tasks,
      BusinessProcessDefinition selectedProcess,
      Map<String, String> historicalActivityNames) {
    Map<Long, Set<String>> activityIds = new LinkedHashMap<>();
    tasks.forEach(
        task -> {
          LinkedHashSet<String> ids = new LinkedHashSet<>();
          if (task.getProcessActivityId() != null && !task.getProcessActivityId().isBlank()) {
            ids.add(task.getProcessActivityId());
          }
          activityIds.put(task.getId(), ids);
        });
    if (tasks.isEmpty()) return activityIds;
    List<AgentTaskActivityCoverage> coverages =
        activityCoverageRepository.findAllByAgentTaskIdIn(
            tasks.stream().map(AgentTask::getId).toList());
    coverages.stream()
        .filter(coverage -> coverage.getActivityDefinition() != null)
        .filter(
            coverage ->
                coverage.getActivityDefinition().getProcessDefinition() != null
                    && selectedProcess
                        .getProcessCode()
                        .equals(
                            coverage
                                .getActivityDefinition()
                                .getProcessDefinition()
                                .getProcessCode()))
        .forEach(
            coverage -> {
              historicalActivityNames.putIfAbsent(
                  coverage.getActivityDefinition().getActivityId(),
                  coverage.getActivityDefinition().getName());
              activityIds
                  .computeIfAbsent(
                      coverage.getAgentTask().getId(), ignored -> new LinkedHashSet<>())
                  .add(coverage.getActivityDefinition().getActivityId());
            });
    return activityIds;
  }

  /** Monta grupos ordenados e mantém atividades da versão atual mesmo quando ainda estão vazias. */
  private List<ProductProcessActivityExecutionGroupResponse> activityGroups(
      Map<String, List<AgentTask>> tasksByActivityId,
      Map<String, BusinessProcessActivityDefinition> selectedByActivityId,
      Map<String, String> historicalActivityNames,
      Map<Long, BusinessProcessActivityExecutionResponse> taskResponses) {
    List<ProductProcessActivityExecutionGroupResponse> groups = new java.util.ArrayList<>();
    int sequence = 1;
    for (Map.Entry<String, List<AgentTask>> entry : tasksByActivityId.entrySet()) {
      BusinessProcessActivityDefinition definition = selectedByActivityId.get(entry.getKey());
      List<BusinessProcessActivityExecutionResponse> executions =
          entry.getValue().stream().map(task -> taskResponses.get(task.getId())).toList();
      String activityName =
          definition != null
              ? definition.getName()
              : UNLINKED_ACTIVITY_ID.equals(entry.getKey())
                  ? "Tarefas sem atividade BPM vinculada"
                  : historicalActivityNames.getOrDefault(entry.getKey(), entry.getKey());
      groups.add(
          new ProductProcessActivityExecutionGroupResponse(
              definition == null ? null : definition.getId(),
              entry.getKey(),
              activityName,
              definition == null ? null : definition.getObjective(),
              definition == null ? null : definition.getOwnerName(),
              sequence++,
              definition != null,
              executions.size(),
              executions));
    }
    return groups;
  }

  /** Soma custo estimado uma única vez por tarefa, mesmo quando ela cobre várias atividades. */
  private BigDecimal knownEstimatedCost(List<AgentTask> tasks) {
    return tasks.stream()
        .map(AgentTask::getEstimatedCostUsd)
        .filter(java.util.Objects::nonNull)
        .reduce(BigDecimal.ZERO.setScale(8), BigDecimal::add)
        .setScale(8, RoundingMode.HALF_UP);
  }

  /** Classifica a cobertura financeira do conjunto único de tarefas do produto. */
  private String costCoverage(List<AgentTask> tasks) {
    if (tasks.isEmpty()) return "NO_EXECUTIONS";
    long covered =
        tasks.stream()
            .filter(
                task ->
                    task.getEstimatedCostUsd() != null
                        || "NOT_APPLICABLE".equals(task.getCostEstimationStatus()))
            .count();
    if (covered == tasks.size()) return "COMPLETE";
    return tasks.stream().anyMatch(task -> task.getEstimatedCostUsd() != null)
        ? "PARTIAL"
        : "NOT_REPORTED";
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
    return response(task, productInternalName(task));
  }

  /** Converte a tarefa usando a identidade de produto já validada pelo contexto da consulta. */
  private BusinessProcessActivityExecutionResponse response(
      AgentTask task, String knownProductInternalName) {
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
        executionReasoningEffort(task, technicalExecution.orElse(null)),
        knownProductInternalName,
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

  /** Recupera o esforço auditado sem deduzi-lo a partir do modelo ou da configuração atual. */
  private String executionReasoningEffort(
      AgentTask task, GeraLandingStageExecution technicalExecution) {
    return firstPresent(
        task.getExecutionReasoningEffort(),
        firstPresent(
            technicalExecution == null ? null : technicalExecution.getExecutionReasoningEffort(),
            evidenceText(task, "reasoningEffort", "modelReasoningEffort")));
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
