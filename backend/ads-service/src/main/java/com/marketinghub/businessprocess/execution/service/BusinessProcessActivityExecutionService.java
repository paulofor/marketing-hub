package com.marketinghub.businessprocess.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskActivityCoverage;
import com.marketinghub.agenttask.AgentTaskAuditView;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskResultView;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionControlResponse;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityRequirementResponse;
import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionHistoryResponse;
import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionResponse;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequestResponse;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskActivityCoverageRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: consultar e solicitar tarefas BPM auditáveis por atividade, processo e produto.
 */
@Service
public class BusinessProcessActivityExecutionService {
  private static final int RECENT_EXECUTION_LIMIT = 10;
  private static final String UNLINKED_ACTIVITY_ID = "__unlinked__";
  private static final Pattern COMMERCIAL_PLAN_REFERENCE =
      Pattern.compile("^commercial-plan:(\\d+)@.*$");
  private static final Pattern EXPERIMENT_REFERENCE = Pattern.compile("^experiment:(\\d+)$");
  private static final Logger LOGGER =
      LoggerFactory.getLogger(BusinessProcessActivityExecutionService.class);

  private final BusinessProcessDefinitionRepository processRepository;
  private final BusinessProcessActivityDefinitionRepository activityDefinitionRepository;
  private final AgentTaskRepository taskRepository;
  private final AgentTaskActivityCoverageRepository activityCoverageRepository;
  private final BusinessProcessActivityInstanceRepository activityInstanceRepository;
  private final CommercialPlanRepository commercialPlanRepository;
  private final GeraLandingStageExecutionRepository landingExecutionRepository;
  private final ProductRepository productRepository;
  private final ExperimentRepository experimentRepository;
  private final AgentTaskService agentTaskService;
  private final ObjectMapper objectMapper;
  private final List<BackendProductProcessActivityExecutor> backendActivityExecutors;
  private final List<HumanProductProcessActivityExecutor> humanActivityExecutors;
  private final List<AgentProductProcessActivityReadinessProvider> agentActivityReadinessProviders;

  /** Configura as fontes canônicas do processo, das tarefas, da cobertura e do produto. */
  @Autowired
  public BusinessProcessActivityExecutionService(
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      AgentTaskRepository taskRepository,
      AgentTaskActivityCoverageRepository activityCoverageRepository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      CommercialPlanRepository commercialPlanRepository,
      GeraLandingStageExecutionRepository landingExecutionRepository,
      ProductRepository productRepository,
      ExperimentRepository experimentRepository,
      AgentTaskService agentTaskService,
      ObjectMapper objectMapper,
      List<BackendProductProcessActivityExecutor> backendActivityExecutors,
      List<HumanProductProcessActivityExecutor> humanActivityExecutors,
      List<AgentProductProcessActivityReadinessProvider> agentActivityReadinessProviders) {
    this.processRepository = processRepository;
    this.activityDefinitionRepository = activityDefinitionRepository;
    this.taskRepository = taskRepository;
    this.activityCoverageRepository = activityCoverageRepository;
    this.activityInstanceRepository = activityInstanceRepository;
    this.commercialPlanRepository = commercialPlanRepository;
    this.landingExecutionRepository = landingExecutionRepository;
    this.productRepository = productRepository;
    this.experimentRepository = experimentRepository;
    this.agentTaskService = agentTaskService;
    this.objectMapper = objectMapper;
    this.backendActivityExecutors = List.copyOf(backendActivityExecutors);
    this.humanActivityExecutors = List.copyOf(humanActivityExecutors);
    this.agentActivityReadinessProviders = List.copyOf(agentActivityReadinessProviders);
  }

  /** Mantém testes que configuram executores backend e gates de agente sem decisão humana. */
  BusinessProcessActivityExecutionService(
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      AgentTaskRepository taskRepository,
      AgentTaskActivityCoverageRepository activityCoverageRepository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      CommercialPlanRepository commercialPlanRepository,
      GeraLandingStageExecutionRepository landingExecutionRepository,
      ProductRepository productRepository,
      ExperimentRepository experimentRepository,
      AgentTaskService agentTaskService,
      ObjectMapper objectMapper,
      List<BackendProductProcessActivityExecutor> backendActivityExecutors,
      List<AgentProductProcessActivityReadinessProvider> agentActivityReadinessProviders) {
    this(
        processRepository,
        activityDefinitionRepository,
        taskRepository,
        activityCoverageRepository,
        activityInstanceRepository,
        commercialPlanRepository,
        landingExecutionRepository,
        productRepository,
        experimentRepository,
        agentTaskService,
        objectMapper,
        backendActivityExecutors,
        List.of(),
        agentActivityReadinessProviders);
  }

  /** Mantém compatibilidade dos testes que não exercitam atividades determinísticas do backend. */
  BusinessProcessActivityExecutionService(
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      AgentTaskRepository taskRepository,
      AgentTaskActivityCoverageRepository activityCoverageRepository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      CommercialPlanRepository commercialPlanRepository,
      GeraLandingStageExecutionRepository landingExecutionRepository,
      ProductRepository productRepository,
      ExperimentRepository experimentRepository,
      AgentTaskService agentTaskService,
      ObjectMapper objectMapper) {
    this(
        processRepository,
        activityDefinitionRepository,
        taskRepository,
        activityCoverageRepository,
        activityInstanceRepository,
        commercialPlanRepository,
        landingExecutionRepository,
        productRepository,
        experimentRepository,
        agentTaskService,
        objectMapper,
        List.of(),
        List.of(),
        List.of());
  }

  /** Permite testes unitários sem carregar o catálogo comercial. */
  BusinessProcessActivityExecutionService(
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskRepository taskRepository,
      ObjectMapper objectMapper) {
    this(
        processRepository,
        null,
        taskRepository,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        objectMapper);
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
        null,
        landingExecutionRepository,
        null,
        null,
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
   * Consolida a situação das atividades da versão selecionada e todas as tarefas do produto no
   * mesmo processo canônico, preservando atividades históricas e cobertura composta.
   */
  @Transactional(readOnly = true)
  public ProductProcessActivityExecutionHistoryResponse productProcessExecutions(
      Long processDefinitionId, Long productId) {
    BusinessProcessDefinition selectedProcess = requiredProcess(processDefinitionId);
    Product product = requiredProduct(productId);
    List<CommercialPlan> productPlans = commercialPlanRepository.findByProductId(productId);
    List<Experiment> productExperiments = productExperiments(productId);
    List<AgentTask> tasks =
        productProcessTasks(
            productPlans, productExperiments, productId, selectedProcess.getProcessCode());
    List<BusinessProcessActivityInstance> instances =
        productProcessActivityInstances(
            productPlans, productExperiments, productId, selectedProcess.getProcessCode());
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
    String currentExecutionReference =
        resolveExecutionReference(selectedProcess, product, productExperiments, tasks, instances);
    String readinessSourceReference =
        currentExecutionReference == null
            ? initialSourceReference(selectedProcess, product, productExperiments)
            : currentExecutionReference;
    boolean hasExecutionContext = readinessSourceReference != null;
    Map<String, List<BusinessProcessActivityInstance>> currentInstancesByActivityId =
        currentInstancesByActivityId(selectedProcess.getId(), currentExecutionReference, instances);
    List<ProductProcessActivityExecutionGroupResponse> activities =
        activityGroups(
            selectedProcess,
            tasksByActivityId,
            selectedByActivityId,
            historicalActivityNames,
            taskResponses,
            currentExecutionReference,
            readinessSourceReference,
            currentInstancesByActivityId,
            product,
            hasExecutionContext,
            !Boolean.FALSE.equals(product.getAutomaticExecutionEnabled()));
    ProductProcessSituation situation = processSituation(activities);
    BigDecimal knownCost = knownEstimatedCost(tasks);
    CommercialPlan commercialPlan = currentCommercialPlan(productPlans, currentExecutionReference);
    return new ProductProcessActivityExecutionHistoryResponse(
        product.getId(),
        product.getName(),
        product.getInternalName(),
        commercialPlan == null ? null : commercialPlan.getId(),
        commercialPlan == null ? null : commercialPlan.getName(),
        selectedProcess.getId(),
        selectedProcess.getProcessCode(),
        selectedProcess.getName(),
        selectedProcess.getVersionNumber(),
        selectedProcess.getStatus(),
        currentExecutionReference,
        situation.operationalState(),
        situation.objectiveAchieved(),
        situation.selectedActivityCount(),
        situation.completedActivityCount(),
        situation.remainingActivityCount(),
        situation.blockedActivityCount(),
        situation.currentActivity() == null ? null : situation.currentActivity().activityId(),
        situation.currentActivity() == null ? null : situation.currentActivity().activityName(),
        situation.currentActivity() == null ? null : situation.currentActivity().operationalState(),
        situation.currentActivity() == null ? null : situation.currentActivity().stateReason(),
        activities.size(),
        (int) activities.stream().filter(activity -> activity.taskCount() > 0).count(),
        tasks.size(),
        knownCost,
        costCoverage(tasks),
        activities);
  }

  /**
   * Resolve o plano comercial do ciclo atual e usa o plano mais recente do produto como fallback.
   */
  private CommercialPlan currentCommercialPlan(
      List<CommercialPlan> productPlans, String currentExecutionReference) {
    if (currentExecutionReference != null) {
      Matcher matcher = COMMERCIAL_PLAN_REFERENCE.matcher(currentExecutionReference);
      if (matcher.matches()) {
        String referencedPlanId = matcher.group(1);
        Optional<CommercialPlan> referencedPlan =
            productPlans.stream()
                .filter(plan -> String.valueOf(plan.getId()).equals(referencedPlanId))
                .findFirst();
        if (referencedPlan.isPresent()) return referencedPlan.get();
      }
    }
    return productPlans.stream().findFirst().orElse(null);
  }

  /** Mantém o contrato sem corpo usado por atividades de agente e comandos backend legados. */
  @Transactional
  public ProductProcessActivityExecutionRequestResponse requestProductActivityExecution(
      Long processDefinitionId, Long productId, String activityId) {
    return requestProductActivityExecution(processDefinitionId, productId, activityId, null);
  }

  /** Inicia a atividade ou registra uma decisão humana auditável após confirmação explícita. */
  @Transactional
  public ProductProcessActivityExecutionRequestResponse requestProductActivityExecution(
      Long processDefinitionId,
      Long productId,
      String activityId,
      ProductProcessActivityExecutionRequest request) {
    if (agentTaskService == null || experimentRepository == null) {
      throw new IllegalStateException("Execução de atividade não configurada neste ambiente.");
    }
    BusinessProcessDefinition process = requiredProcess(processDefinitionId);
    if (!"PUBLISHED".equals(process.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente a versão publicada pode iniciar uma atividade.");
    }
    Product product = requiredProduct(productId);
    if (Boolean.FALSE.equals(product.getAutomaticExecutionEnabled())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O produto está em STOP e não pode iniciar novas atividades.");
    }
    JsonNode activity = requireTaskActivity(process, activityId);
    String normalizedActivityId = activity.path("id").asText();
    BusinessProcessActivityDefinition activityDefinition =
        activityDefinitionRepository
            .findByProcessDefinitionIdAndActivityId(process.getId(), normalizedActivityId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A atividade publicada não possui definição operacional persistida."));
    List<String> responsibleAgentKeys = responsibleAgentKeys(activityDefinition);
    Optional<BackendProductProcessActivityExecutor> backendExecutor =
        backendActivityExecutor(process, activityDefinition);
    Optional<HumanProductProcessActivityExecutor> humanExecutor =
        humanActivityExecutor(process, activityDefinition);
    if (responsibleAgentKeys.isEmpty() && backendExecutor.isEmpty() && humanExecutor.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A atividade não possui contrato de execução configurado.");
    }
    List<Experiment> productExperiments = productExperiments(productId);
    List<CommercialPlan> productPlans = commercialPlanRepository.findByProductId(productId);
    List<AgentTask> processTasks =
        productProcessTasks(productPlans, productExperiments, productId, process.getProcessCode());
    List<BusinessProcessActivityInstance> processInstances =
        productProcessActivityInstances(
            productPlans, productExperiments, productId, process.getProcessCode());
    String currentSourceReference =
        resolveExecutionReference(
            process, product, productExperiments, processTasks, processInstances);
    String sourceReference =
        currentSourceReference == null
            ? initialSourceReference(process, product, productExperiments)
            : currentSourceReference;
    if (sourceReference == null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O produto ainda não possui experimento nem contrato privado para contextualizar a execução.");
    }
    ActivitySituation currentSituation =
        activitySituation(
            normalizedActivityId,
            processTasks.stream()
                .filter(task -> sourceReference.equals(task.getSourceReference()))
                .filter(task -> normalizedActivityId.equals(task.getProcessActivityId()))
                .toList(),
            currentInstancesByActivityId(process.getId(), sourceReference, processInstances)
                .getOrDefault(normalizedActivityId, List.of()));
    requireRequestableActivityState(currentSituation.operationalState());
    if (backendExecutor.isPresent()) {
      BackendProductProcessActivityReadiness readiness =
          backendExecutor.get().readiness(process, activityDefinition, product, sourceReference);
      if (!readiness.ready()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, readiness.reason());
      }
      BackendProductProcessActivityExecutionResult result =
          backendExecutor.get().execute(process, activityDefinition, product, sourceReference);
      return new ProductProcessActivityExecutionRequestResponse(
          process.getId(),
          product.getId(),
          normalizedActivityId,
          result.sourceReference(),
          List.of(),
          result.operationalState(),
          result.objectiveAchieved(),
          result.message());
    }
    if (humanExecutor.isPresent()) {
      HumanProductProcessActivityReadiness readiness =
          humanExecutor.get().readiness(process, activityDefinition, product, sourceReference);
      if (!readiness.ready()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, readiness.reason());
      }
      HumanProductProcessActivityExecutionResult result =
          humanExecutor
              .get()
              .execute(process, activityDefinition, product, sourceReference, request);
      return new ProductProcessActivityExecutionRequestResponse(
          process.getId(),
          product.getId(),
          normalizedActivityId,
          result.sourceReference(),
          List.of(),
          result.operationalState(),
          result.objectiveAchieved(),
          result.message());
    }
    List<AgentProductProcessActivityReadinessProvider> agentReadinessProviders =
        agentActivityReadinessProviders(process, activityDefinition);
    AgentProductProcessActivityReadiness agentReadiness =
        combinedAgentActivityReadiness(
            agentReadinessProviders, process, activityDefinition, product, sourceReference);
    if (agentReadiness != null && !agentReadiness.ready()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, agentReadiness.reason());
    }
    String activityName = activity.path("label").asText(normalizedActivityId);
    String objective = activity.path("description").asText(activityName);
    List<AgentTaskResponse> requestedTasks =
        responsibleAgentKeys.stream()
            .map(
                agentKey ->
                    agentTaskService.retryBlockedByHumanOrRefreshPending(
                        new CreateAgentTaskRequest(
                            agentKey,
                            "Operador do Marketing Hub",
                            activityName + " · " + productDisplayName(product),
                            objective,
                            "HIGH",
                            sourceReference,
                            process.getId(),
                            normalizedActivityId,
                            false,
                            null)))
            .toList();
    return new ProductProcessActivityExecutionRequestResponse(
        process.getId(), product.getId(), normalizedActivityId, sourceReference, requestedTasks);
  }

  /** Aceita somente atividade inédita ou bloqueada, impedindo reinício de trabalho ainda ativo. */
  private void requireRequestableActivityState(String operationalState) {
    if ("NOT_STARTED".equals(operationalState) || "BLOCKED".equals(operationalState)) return;
    if ("COMPLETED".equals(operationalState)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O objetivo da atividade já foi atingido neste ciclo.");
    }
    throw new ResponseStatusException(
        HttpStatus.CONFLICT, "A atividade já possui execução ativa neste ciclo.");
  }

  /** Exige um produto existente antes de resolver suas referências operacionais. */
  private Product requiredProduct(Long productId) {
    return productRepository
        .findById(productId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
  }

  /** Lista as referências de experimento que pertencem diretamente ao produto. */
  private List<Experiment> productExperiments(Long productId) {
    return experimentRepository == null
        ? List.of()
        : experimentRepository.findByProductIdOrderByUpdatedAtDescIdDesc(productId);
  }

  /** Resolve a referência inicial sem fabricar experimento para um protótipo pré-comercial. */
  private String initialSourceReference(
      BusinessProcessDefinition process, Product product, List<Experiment> productExperiments) {
    if ("operacao-otimizacao-experimento".equals(process.getProcessCode())) {
      Optional<Experiment> running =
          productExperiments.stream()
              .filter(experiment -> experiment.getStatus() == ExperimentStatus.RUNNING)
              .findFirst();
      Optional<Experiment> alreadyOperated =
          productExperiments.stream()
              .filter(experiment -> experiment.getStatus() != ExperimentStatus.PLANNED)
              .findFirst();
      return running
          .or(() -> alreadyOperated)
          .map(experiment -> "experiment:" + experiment.getId())
          .orElse(null);
    }
    if (!productExperiments.isEmpty()) {
      return "experiment:" + productExperiments.getFirst().getId();
    }
    if ("pde-construction-approval".equals(process.getProcessCode())
        && "PDE_PRIVATE_VALIDATION_V1".equals(product.getValidationDefinitionVersion())) {
      return "product:" + product.getId() + "@private-validation-v1";
    }
    if ("pde-construction-approval".equals(process.getProcessCode())
        && usesPdeAgentValidationV1(product)) {
      return "product:" + product.getId() + "@agent-validation-v1";
    }
    return null;
  }

  /**
   * Mantém o processo de operação vinculado ao experimento efetivamente iniciado, mesmo quando um
   * sucessor planejado recebeu por engano uma tentativa mais recente.
   */
  private String resolveExecutionReference(
      BusinessProcessDefinition process,
      Product product,
      List<Experiment> productExperiments,
      List<AgentTask> tasks,
      List<BusinessProcessActivityInstance> instances) {
    if ("operacao-otimizacao-experimento".equals(process.getProcessCode())) {
      return initialSourceReference(process, product, productExperiments);
    }
    if ("pde-construction-approval".equals(process.getProcessCode())
        && usesPdeAgentValidationV1(product)) {
      return initialSourceReference(process, product, productExperiments);
    }
    return currentExecutionReference(tasks, instances);
  }

  /** Limita a referência imutável da execução aos dois estados previstos pelo contrato v1. */
  private boolean usesPdeAgentValidationV1(Product product) {
    return product != null
        && Set.of("PDE_AGENT_VALIDATION_V1", "PDE_AGENT_VALIDATED_V1")
            .contains(product.getValidationDefinitionVersion());
  }

  /** Prefere o nome interno no título operacional sem expor ausência como texto vazio. */
  private String productDisplayName(Product product) {
    if (product.getInternalName() != null && !product.getInternalName().isBlank()) {
      return product.getInternalName();
    }
    return product.getName() == null || product.getName().isBlank()
        ? "Produto " + product.getId()
        : product.getName();
  }

  /** Lê da definição relacional versionada todos os agentes que precisam concluir a atividade. */
  private List<String> responsibleAgentKeys(BusinessProcessActivityDefinition definition) {
    if (definition.getDefinitionJson() == null || definition.getDefinitionJson().isBlank()) {
      return List.of();
    }
    try {
      return responsibleAgentKeys(objectMapper.readTree(definition.getDefinitionJson()));
    } catch (Exception ex) {
      LOGGER.error(
          "Falha ao ler executores da atividade BPM. activityDefinitionId={} activityId={}",
          definition.getId(),
          definition.getActivityId(),
          ex);
      return List.of();
    }
  }

  /** Valida a lista versionada de executores sem aceitar valores vazios ou duplicados. */
  private List<String> responsibleAgentKeys(JsonNode activity) {
    JsonNode values = activity.path("responsibleAgentKeys");
    if (!values.isArray()) return List.of();
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    values.forEach(
        value -> {
          String key = textOrNull(value);
          if (key != null) keys.add(key);
        });
    return List.copyOf(keys);
  }

  /** Explica por que a tela pode ou não solicitar a execução dessa atividade. */
  private String executionRequestReason(
      BusinessProcessActivityDefinition definition,
      BusinessProcessDefinition process,
      String operationalState,
      List<String> responsibleAgents,
      boolean hasBackendExecutor,
      BackendProductProcessActivityReadiness backendReadiness,
      boolean hasHumanExecutor,
      HumanProductProcessActivityReadiness humanReadiness,
      boolean hasAgentReadinessProvider,
      AgentProductProcessActivityReadiness agentReadiness,
      boolean hasExecutionContext,
      boolean productExecutionEnabled) {
    if (definition == null) return "Atividade histórica sem comando operacional.";
    if (!"PUBLISHED".equals(process.getStatus())) {
      return "Somente a versão publicada pode receber novas execuções.";
    }
    if ("COMPLETED".equals(operationalState)) {
      return "O objetivo da atividade já foi atingido neste ciclo.";
    }
    if (!"NOT_STARTED".equals(operationalState) && !"BLOCKED".equals(operationalState)) {
      return "A atividade já possui execução registrada neste ciclo.";
    }
    if (!hasExecutionContext) {
      return "O produto ainda não possui experimento nem contrato privado para contextualizar a execução.";
    }
    if (!productExecutionEnabled) {
      return "O produto está em STOP e não pode iniciar novas atividades.";
    }
    if (hasBackendExecutor && backendReadiness != null && !backendReadiness.ready()) {
      return backendReadiness.reason();
    }
    if (hasHumanExecutor && humanReadiness != null && !humanReadiness.ready()) {
      return humanReadiness.reason();
    }
    if (hasAgentReadinessProvider && agentReadiness != null && !agentReadiness.ready()) {
      return agentReadiness.reason();
    }
    if (responsibleAgents.isEmpty() && !hasBackendExecutor && !hasHumanExecutor) {
      return "A atividade não possui contrato de execução configurado.";
    }
    if ("BLOCKED".equals(operationalState)) {
      return "A tentativa bloqueada será preservada e uma nova tarefa será aberta.";
    }
    if (hasBackendExecutor && backendReadiness != null) return backendReadiness.reason();
    if (hasHumanExecutor && humanReadiness != null) return humanReadiness.reason();
    if (hasAgentReadinessProvider && agentReadiness != null) return agentReadiness.reason();
    return "A atividade está pronta para abrir todas as tarefas responsáveis.";
  }

  /** Monta o comando uniforme de agente, backend, subprocesso ou aprovação humana. */
  private ProductProcessActivityExecutionControlResponse executionControl(
      BusinessProcessActivityDefinition definition,
      BusinessProcessDefinition process,
      String operationalState,
      List<String> responsibleAgents,
      boolean hasBackendExecutor,
      BackendProductProcessActivityReadiness backendReadiness,
      boolean hasHumanExecutor,
      HumanProductProcessActivityReadiness humanReadiness,
      boolean requestAvailable,
      String requestReason,
      boolean hasExecutionContext,
      boolean productExecutionEnabled) {
    if (definition == null) {
      return new ProductProcessActivityExecutionControlResponse(
          "HISTORICAL",
          "STATUS",
          null,
          "Atividade histórica preservada somente para auditoria.",
          false,
          requestReason,
          false,
          null,
          null,
          null,
          null,
          null,
          null,
          List.of());
    }
    if (hasHumanExecutor) {
      if (humanReadiness == null) {
        return new ProductProcessActivityExecutionControlResponse(
            "HUMAN",
            "APPROVAL",
            "Registrar decisão",
            "A atividade exige uma decisão humana explícita e auditável.",
            false,
            requestReason,
            true,
            "Confirmar decisão humana",
            "Revise objetivo, evidências e impacto antes de decidir.",
            null,
            null,
            null,
            null,
            List.of());
      }
      List<ProductProcessActivityRequirementResponse> requirements =
          humanReadiness.requirements().stream()
              .map(
                  requirement ->
                      new ProductProcessActivityRequirementResponse(
                          requirement.code(),
                          requirement.title(),
                          requirement.satisfied(),
                          requirement.detail(),
                          requirement.recommendation()))
              .toList();
      return new ProductProcessActivityExecutionControlResponse(
          "HUMAN",
          "APPROVAL",
          humanReadiness.actionLabel(),
          humanReadiness.description(),
          requestAvailable,
          requestReason,
          true,
          humanReadiness.confirmationTitle(),
          humanReadiness.confirmationMessage(),
          humanReadiness.confirmationToken(),
          humanReadiness.workspaceCode(),
          humanReadiness.workspaceReferenceId(),
          null,
          requirements,
          humanReadiness.decisionMode(),
          humanReadiness.auditEvidenceReference());
    }
    if (hasBackendExecutor) {
      if (backendReadiness == null) {
        return new ProductProcessActivityExecutionControlResponse(
            "BACKEND",
            "COMMAND",
            null,
            "O backend possui um comando determinístico para esta atividade.",
            false,
            requestReason,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of());
      }
      return new ProductProcessActivityExecutionControlResponse(
          "BACKEND",
          backendReadiness.workspaceCode() == null ? "COMMAND" : "WORKSPACE",
          backendReadiness.actionLabel(),
          backendReadiness.description(),
          requestAvailable,
          requestReason,
          false,
          null,
          null,
          null,
          backendReadiness.workspaceCode(),
          backendReadiness.workspaceReferenceId(),
          null,
          backendReadiness.requirements());
    }
    if (!responsibleAgents.isEmpty()) {
      return new ProductProcessActivityExecutionControlResponse(
          "AGENT",
          "COMMAND",
          "BLOCKED".equals(operationalState) ? "Reiniciar tarefa" : "Executar atividade",
          "Abre todas as tarefas responsáveis no mesmo ciclo auditável.",
          requestAvailable,
          requestReason,
          false,
          null,
          null,
          null,
          null,
          null,
          null,
          List.of());
    }
    Optional<BusinessProcessDefinition> subprocess = publishedSubprocess(definition);
    if (subprocess.isPresent()) {
      boolean navigationAvailable =
          "PUBLISHED".equals(process.getStatus())
              && hasExecutionContext
              && productExecutionEnabled
              && !"COMPLETED".equals(operationalState);
      String reason =
          navigationAvailable
              ? "Abra o subprocesso oficial; ele preserva tarefas, evidências e custos próprios."
              : requestReason;
      return new ProductProcessActivityExecutionControlResponse(
          "BACKEND",
          "SUBPROCESS",
          "Abrir subprocesso",
          "A atividade delega a execução ao subprocesso publicado e será concluída pelo backend a partir do resultado persistido.",
          navigationAvailable,
          reason,
          false,
          null,
          null,
          null,
          null,
          null,
          subprocess.get().getId(),
          List.of());
    }
    if (isBackendOwner(definition)) {
      return new ProductProcessActivityExecutionControlResponse(
          "BACKEND",
          "AUTOMATIC",
          null,
          "O backend deve concluir esta atividade a partir do evento ou resultado técnico correspondente.",
          false,
          "Nenhum comando manual é seguro para esta atividade; o backend ainda não recebeu o evento conclusivo.",
          false,
          null,
          null,
          null,
          null,
          null,
          null,
          List.of());
    }
    return new ProductProcessActivityExecutionControlResponse(
        "UNCONFIGURED",
        "STATUS",
        null,
        "A definição ainda não declarou uma forma operacional de execução.",
        false,
        requestReason,
        false,
        null,
        null,
        null,
        null,
        null,
        null,
        List.of());
  }

  /** Resolve a versão publicada do subprocesso sem expor código técnico como ação ao usuário. */
  private Optional<BusinessProcessDefinition> publishedSubprocess(
      BusinessProcessActivityDefinition definition) {
    if (definition.getSubprocessCode() == null || definition.getSubprocessCode().isBlank()) {
      return Optional.empty();
    }
    return processRepository.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
        definition.getSubprocessCode(), "PUBLISHED");
  }

  /** Reconhece responsabilidades explícitas do backend sem delegar inferência ao frontend. */
  private boolean isBackendOwner(BusinessProcessActivityDefinition definition) {
    return definition.getOwnerName() != null
        && definition.getOwnerName().toLowerCase(java.util.Locale.ROOT).contains("backend");
  }

  /** Localiza o único executor backend compatível com a atividade publicada. */
  private Optional<BackendProductProcessActivityExecutor> backendActivityExecutor(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    List<BackendProductProcessActivityExecutor> compatible =
        backendActivityExecutors.stream()
            .filter(executor -> executor.supports(process, activityDefinition))
            .toList();
    if (compatible.size() > 1) {
      throw new IllegalStateException(
          "Mais de um executor backend atende à atividade "
              + activityDefinition.getActivityId()
              + " do processo "
              + process.getProcessCode()
              + ".");
    }
    return compatible.stream().findFirst();
  }

  /** Localiza a única autoridade humana compatível com a atividade publicada. */
  private Optional<HumanProductProcessActivityExecutor> humanActivityExecutor(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    List<HumanProductProcessActivityExecutor> compatible =
        humanActivityExecutors.stream()
            .filter(executor -> executor.supports(process, activityDefinition))
            .toList();
    if (compatible.size() > 1) {
      throw new IllegalStateException(
          "Mais de um executor humano atende à atividade "
              + activityDefinition.getActivityId()
              + " do processo "
              + process.getProcessCode()
              + ".");
    }
    return compatible.stream().findFirst();
  }

  /** Localiza todos os gates independentes aplicáveis à atividade atribuída a agente. */
  private List<AgentProductProcessActivityReadinessProvider> agentActivityReadinessProviders(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return agentActivityReadinessProviders.stream()
        .filter(provider -> provider.supports(process, activityDefinition))
        .toList();
  }

  /** Compõe os gates aplicáveis e preserva todos os motivos de bloqueio sem depender da ordem. */
  private AgentProductProcessActivityReadiness combinedAgentActivityReadiness(
      List<AgentProductProcessActivityReadinessProvider> providers,
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    if (providers.isEmpty()) return null;
    List<AgentProductProcessActivityReadiness> evaluations =
        providers.stream()
            .map(
                provider ->
                    provider.readiness(process, activityDefinition, product, sourceReference))
            .toList();
    boolean ready = evaluations.stream().allMatch(AgentProductProcessActivityReadiness::ready);
    String reason =
        evaluations.stream()
            .filter(evaluation -> ready || !evaluation.ready())
            .map(AgentProductProcessActivityReadiness::reason)
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .collect(Collectors.joining(" "));
    return new AgentProductProcessActivityReadiness(
        ready,
        reason.isBlank()
            ? ready
                ? "Todos os pré-requisitos da atividade estão prontos."
                : "A atividade possui pré-requisito pendente."
            : reason);
  }

  /** Busca tarefas do produto, de seus planos e experimentos sem misturar outro processo. */
  private List<AgentTask> productProcessTasks(
      List<CommercialPlan> productPlans,
      List<Experiment> productExperiments,
      Long productId,
      String processCode) {
    Map<Long, AgentTask> uniqueTasks = new LinkedHashMap<>();
    for (CommercialPlan plan : productPlans) {
      taskRepository
          .findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
              "commercial-plan:" + plan.getId() + "@")
          .stream()
          .filter(task -> task.getProcessDefinition() != null)
          .filter(task -> processCode.equals(task.getProcessDefinition().getProcessCode()))
          .forEach(task -> uniqueTasks.put(task.getId(), task));
    }
    for (Experiment experiment : productExperiments) {
      taskRepository
          .findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:" + experiment.getId())
          .stream()
          .filter(task -> task.getProcessDefinition() != null)
          .filter(task -> processCode.equals(task.getProcessDefinition().getProcessCode()))
          .forEach(task -> uniqueTasks.put(task.getId(), task));
    }
    taskRepository
        .findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("product:" + productId + "@")
        .stream()
        .filter(task -> task.getProcessDefinition() != null)
        .filter(task -> processCode.equals(task.getProcessDefinition().getProcessCode()))
        .forEach(task -> uniqueTasks.put(task.getId(), task));
    return uniqueTasks.values().stream()
        .sorted(
            Comparator.comparing(
                    AgentTask::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AgentTask::getId, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  /** Busca ocorrências BPM do produto, dos planos e experimentos, inclusive sem tarefa. */
  private List<BusinessProcessActivityInstance> productProcessActivityInstances(
      List<CommercialPlan> productPlans,
      List<Experiment> productExperiments,
      Long productId,
      String processCode) {
    Map<Long, BusinessProcessActivityInstance> uniqueInstances = new LinkedHashMap<>();
    for (CommercialPlan plan : productPlans) {
      activityInstanceRepository
          .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceStartingWithOrderByCreatedAtDescIdDesc(
              processCode, "commercial-plan:" + plan.getId() + "@")
          .forEach(instance -> uniqueInstances.put(instance.getId(), instance));
    }
    for (Experiment experiment : productExperiments) {
      activityInstanceRepository
          .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
              processCode, "experiment:" + experiment.getId())
          .forEach(instance -> uniqueInstances.put(instance.getId(), instance));
    }
    activityInstanceRepository
        .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceStartingWithOrderByCreatedAtDescIdDesc(
            processCode, "product:" + productId + "@")
        .forEach(instance -> uniqueInstances.put(instance.getId(), instance));
    return uniqueInstances.values().stream()
        .sorted(
            Comparator.comparing(
                    BusinessProcessActivityInstance::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                    BusinessProcessActivityInstance::getId,
                    Comparator.nullsLast(Comparator.reverseOrder())))
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
      BusinessProcessDefinition selectedProcess,
      Map<String, List<AgentTask>> tasksByActivityId,
      Map<String, BusinessProcessActivityDefinition> selectedByActivityId,
      Map<String, String> historicalActivityNames,
      Map<Long, BusinessProcessActivityExecutionResponse> taskResponses,
      String currentExecutionReference,
      String readinessSourceReference,
      Map<String, List<BusinessProcessActivityInstance>> currentInstancesByActivityId,
      Product product,
      boolean hasExecutionContext,
      boolean productExecutionEnabled) {
    List<ProductProcessActivityExecutionGroupResponse> groups = new java.util.ArrayList<>();
    int sequence = 1;
    for (Map.Entry<String, List<AgentTask>> entry : tasksByActivityId.entrySet()) {
      BusinessProcessActivityDefinition definition = selectedByActivityId.get(entry.getKey());
      List<BusinessProcessActivityExecutionResponse> executions =
          entry.getValue().stream().map(task -> taskResponses.get(task.getId())).toList();
      List<AgentTask> currentExecutionTasks =
          entry.getValue().stream()
              .filter(
                  task ->
                      currentExecutionReference == null
                          || currentExecutionReference.equals(task.getSourceReference()))
              .toList();
      ActivitySituation situation =
          activitySituation(
              entry.getKey(),
              currentExecutionTasks,
              currentInstancesByActivityId.getOrDefault(entry.getKey(), List.of()));
      List<String> responsibleAgents =
          definition == null ? List.of() : responsibleAgentKeys(definition);
      Optional<BackendProductProcessActivityExecutor> backendExecutor =
          definition == null
              ? Optional.empty()
              : backendActivityExecutor(selectedProcess, definition);
      BackendProductProcessActivityReadiness backendReadiness =
          backendExecutor
              .filter(ignored -> hasExecutionContext && readinessSourceReference != null)
              .map(
                  executor ->
                      executor.readiness(
                          selectedProcess, definition, product, readinessSourceReference))
              .orElse(null);
      Optional<HumanProductProcessActivityExecutor> humanExecutor =
          definition == null
              ? Optional.empty()
              : humanActivityExecutor(selectedProcess, definition);
      HumanProductProcessActivityReadiness humanReadiness =
          humanExecutor
              .filter(ignored -> hasExecutionContext && readinessSourceReference != null)
              .map(
                  executor ->
                      executor.readiness(
                          selectedProcess, definition, product, readinessSourceReference))
              .orElse(null);
      List<AgentProductProcessActivityReadinessProvider> agentReadinessProviders =
          definition == null
              ? List.of()
              : agentActivityReadinessProviders(selectedProcess, definition);
      AgentProductProcessActivityReadiness agentReadiness =
          hasExecutionContext && readinessSourceReference != null
              ? combinedAgentActivityReadiness(
                  agentReadinessProviders,
                  selectedProcess,
                  definition,
                  product,
                  readinessSourceReference)
              : null;
      boolean backendStateAllowsRequest =
          "NOT_STARTED".equals(situation.operationalState())
              || "BLOCKED".equals(situation.operationalState());
      boolean agentStateAllowsRequest =
          "NOT_STARTED".equals(situation.operationalState())
              || "BLOCKED".equals(situation.operationalState());
      boolean humanStateAllowsRequest =
          "NOT_STARTED".equals(situation.operationalState())
              || "BLOCKED".equals(situation.operationalState());
      boolean executionRequestAvailable =
          definition != null
              && "PUBLISHED".equals(selectedProcess.getStatus())
              && hasExecutionContext
              && productExecutionEnabled
              && ((!responsibleAgents.isEmpty()
                      && agentStateAllowsRequest
                      && (agentReadiness == null || agentReadiness.ready()))
                  || (backendExecutor.isPresent()
                      && backendStateAllowsRequest
                      && backendReadiness != null
                      && backendReadiness.ready())
                  || (humanExecutor.isPresent()
                      && humanStateAllowsRequest
                      && humanReadiness != null
                      && humanReadiness.ready()));
      String executionRequestReason =
          executionRequestReason(
              definition,
              selectedProcess,
              situation.operationalState(),
              responsibleAgents,
              backendExecutor.isPresent(),
              backendReadiness,
              humanExecutor.isPresent(),
              humanReadiness,
              !agentReadinessProviders.isEmpty(),
              agentReadiness,
              hasExecutionContext,
              productExecutionEnabled);
      ProductProcessActivityExecutionControlResponse executionControl =
          executionControl(
              definition,
              selectedProcess,
              situation.operationalState(),
              responsibleAgents,
              backendExecutor.isPresent(),
              backendReadiness,
              humanExecutor.isPresent(),
              humanReadiness,
              executionRequestAvailable,
              executionRequestReason,
              hasExecutionContext,
              productExecutionEnabled);
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
              situation.operationalState(),
              situation.stateReason(),
              situation.objectiveAchieved(),
              situation.stateEvidence(),
              situation.activityInstanceId(),
              situation.occurrenceNumber(),
              executions.size(),
              executions,
              executionRequestAvailable,
              executionRequestReason,
              executionControl));
    }
    return groups;
  }

  /**
   * Identifica a execução mais recente do processo para impedir que um ciclo antigo masque a
   * situação atual do produto.
   */
  private String currentExecutionReference(
      List<AgentTask> tasks, List<BusinessProcessActivityInstance> instances) {
    return java.util.stream.Stream.concat(
            tasks.stream()
                .map(
                    task ->
                        new ExecutionReferenceCandidate(
                            task.getSourceReference(), task.getCreatedAt(), task.getId())),
            instances.stream()
                .map(
                    instance ->
                        new ExecutionReferenceCandidate(
                            instance.getSourceReference(),
                            instance.getCreatedAt(),
                            instance.getId())))
        .filter(candidate -> candidate.sourceReference() != null)
        .filter(candidate -> !candidate.sourceReference().isBlank())
        .sorted(
            Comparator.comparing(
                    ExecutionReferenceCandidate::createdAt,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                    ExecutionReferenceCandidate::id,
                    Comparator.nullsLast(Comparator.reverseOrder())))
        .map(ExecutionReferenceCandidate::sourceReference)
        .findFirst()
        .orElse(null);
  }

  /** Agrupa as ocorrências da versão selecionada que pertencem ao ciclo operacional atual. */
  private Map<String, List<BusinessProcessActivityInstance>> currentInstancesByActivityId(
      Long processDefinitionId,
      String currentExecutionReference,
      List<BusinessProcessActivityInstance> instances) {
    Map<String, List<BusinessProcessActivityInstance>> byActivityId = new LinkedHashMap<>();
    if (currentExecutionReference == null) return byActivityId;
    instances.stream()
        .filter(instance -> currentExecutionReference.equals(instance.getSourceReference()))
        .filter(instance -> instance.getActivityDefinition() != null)
        .filter(instance -> instance.getActivityDefinition().getProcessDefinition() != null)
        .filter(
            instance ->
                processDefinitionId.equals(
                    instance.getActivityDefinition().getProcessDefinition().getId()))
        .forEach(
            instance ->
                byActivityId
                    .computeIfAbsent(
                        instance.getActivityDefinition().getActivityId(),
                        ignored -> new java.util.ArrayList<>())
                    .add(instance));
    return byActivityId;
  }

  /** Usa primeiro a instância BPM da própria atividade e mantém fallback explícito para legados. */
  private ActivitySituation activitySituation(
      String activityId,
      List<AgentTask> activityTasks,
      List<BusinessProcessActivityInstance> activityInstances) {
    Optional<BusinessProcessActivityInstance> latestInstance =
        latestMatchingActivityInstance(activityId, activityTasks, activityInstances);
    if (latestInstance.isPresent()) {
      BusinessProcessActivityInstance instance = latestInstance.get();
      return new ActivitySituation(
          instance.getStatus(),
          activityInstanceStateReason(instance),
          instance.isObjectiveAchieved(),
          instance.getEvidenceQuality(),
          instance.getId(),
          instance.getOccurrenceNumber());
    }
    if (activityTasks.isEmpty()) {
      return new ActivitySituation(
          "NOT_STARTED",
          "Nenhuma tarefa ou instância foi registrada para esta atividade.",
          false,
          "NOT_RECORDED",
          null,
          null);
    }

    List<AgentTask> currentAttempts = currentTaskAttempts(activityTasks);
    String operationalState = aggregateTaskStatus(currentAttempts);
    AgentTask latestTask = activityTasks.getFirst();
    boolean directLegacyTask = activityId.equals(latestTask.getProcessActivityId());
    String evidence = directLegacyTask ? "LEGACY_TASK" : "COMPOSITE_TASK_COVERAGE";
    return new ActivitySituation(
        operationalState,
        taskStateReason(operationalState, evidence, currentAttempts, latestTask),
        "COMPLETED".equals(operationalState),
        evidence,
        null,
        null);
  }

  /** Seleciona somente a ocorrência mais recente vinculada diretamente à atividade consultada. */
  private Optional<BusinessProcessActivityInstance> latestMatchingActivityInstance(
      String activityId,
      List<AgentTask> activityTasks,
      List<BusinessProcessActivityInstance> activityInstances) {
    return java.util.stream.Stream.concat(
            activityInstances.stream(),
            activityTasks.stream().map(AgentTask::getActivityInstance).filter(Objects::nonNull))
        .filter(Objects::nonNull)
        .filter(instance -> instance.getActivityDefinition() != null)
        .filter(instance -> activityId.equals(instance.getActivityDefinition().getActivityId()))
        .max(
            Comparator.comparing(
                    BusinessProcessActivityInstance::getUpdatedAt,
                    Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(
                    BusinessProcessActivityInstance::getId,
                    Comparator.nullsFirst(Comparator.naturalOrder())));
  }

  /**
   * Mantém a tentativa mais recente de cada responsável ao projetar tarefas legadas ou compostas.
   */
  private List<AgentTask> currentTaskAttempts(List<AgentTask> activityTasks) {
    Map<String, AgentTask> latestByAgent = new LinkedHashMap<>();
    activityTasks.forEach(
        task -> latestByAgent.putIfAbsent(task.getAssignedAgent().getAgentKey(), task));
    return List.copyOf(latestByAgent.values());
  }

  /** Consolida os estados persistidos das tentativas quando não existe instância da atividade. */
  private String aggregateTaskStatus(List<AgentTask> attempts) {
    if (attempts.stream().anyMatch(task -> "IN_PROGRESS".equals(task.getStatus()))) {
      return "IN_PROGRESS";
    }
    if (attempts.stream().anyMatch(task -> "BLOCKED".equals(task.getStatus()))) {
      return "BLOCKED";
    }
    if (attempts.stream().anyMatch(task -> "PENDING".equals(task.getStatus()))) {
      return "PENDING";
    }
    if (!attempts.isEmpty()
        && attempts.stream().allMatch(task -> "COMPLETED".equals(task.getStatus()))) {
      return "COMPLETED";
    }
    return "CANCELLED";
  }

  /** Explica o estado consolidado pela instância sem substituir a causa persistida de bloqueio. */
  private String activityInstanceStateReason(BusinessProcessActivityInstance instance) {
    return switch (instance.getStatus()) {
      case "COMPLETED" -> "Objetivo da atividade atingido na instância BPM.";
      case "BLOCKED" ->
          firstPresent(
              instance.getBlockedReason(),
              "Atividade bloqueada por uma tentativa ainda não resolvida.");
      case "IN_PROGRESS" -> "Atividade em execução pelo responsável.";
      case "PENDING" -> "Atividade aguardando execução ou liberação pelo backend.";
      case "CANCELLED" -> "Atividade encerrada sem atingir o objetivo.";
      default -> "Estado operacional registrado na instância BPM.";
    };
  }

  /**
   * Explica o fallback auditável usado para tarefas compostas e registros anteriores à instância.
   */
  private String taskStateReason(
      String operationalState,
      String evidence,
      List<AgentTask> currentAttempts,
      AgentTask latestTask) {
    return switch (operationalState) {
      case "COMPLETED" ->
          "COMPOSITE_TASK_COVERAGE".equals(evidence)
              ? "Atividade comprovadamente coberta pela tarefa composta #"
                  + latestTask.getId()
                  + "."
              : "Atividade concluída em tarefa legada, sem instância BPM vinculada.";
      case "BLOCKED" ->
          currentAttempts.stream()
              .filter(task -> "BLOCKED".equals(task.getStatus()))
              .map(AgentTask::getExecutionError)
              .filter(Objects::nonNull)
              .filter(value -> !value.isBlank())
              .findFirst()
              .orElse("Atividade bloqueada por uma tarefa ainda não resolvida.");
      case "IN_PROGRESS" -> "Atividade em execução pelo responsável.";
      case "PENDING" -> "Atividade aguardando execução ou liberação pelo backend.";
      case "CANCELLED" -> "Atividade encerrada sem atingir o objetivo.";
      default -> "Estado consolidado pelas tarefas persistidas.";
    };
  }

  /** Resume o progresso da versão selecionada e aponta a atividade que exige atenção primeiro. */
  private ProductProcessSituation processSituation(
      List<ProductProcessActivityExecutionGroupResponse> activities) {
    List<ProductProcessActivityExecutionGroupResponse> selectedActivities =
        activities.stream()
            .filter(ProductProcessActivityExecutionGroupResponse::selectedVersionActivity)
            .toList();
    int completed =
        (int)
            selectedActivities.stream()
                .filter(ProductProcessActivityExecutionGroupResponse::objectiveAchieved)
                .count();
    int blocked =
        (int)
            selectedActivities.stream()
                .filter(activity -> "BLOCKED".equals(activity.operationalState()))
                .count();
    int remaining = selectedActivities.size() - completed;
    boolean objectiveAchieved = !selectedActivities.isEmpty() && remaining == 0;
    String operationalState =
        processOperationalState(selectedActivities, objectiveAchieved, completed, blocked);
    ProductProcessActivityExecutionGroupResponse currentActivity =
        objectiveAchieved ? null : currentActivity(selectedActivities);
    return new ProductProcessSituation(
        operationalState,
        objectiveAchieved,
        selectedActivities.size(),
        completed,
        remaining,
        blocked,
        currentActivity);
  }

  /** Classifica a situação geral sem transformar atividade técnica em objetivo de processo. */
  private String processOperationalState(
      List<ProductProcessActivityExecutionGroupResponse> activities,
      boolean objectiveAchieved,
      int completed,
      int blocked) {
    if (activities.isEmpty()) return "NOT_RECORDED";
    if (objectiveAchieved) return "COMPLETED";
    if (blocked > 0) return "BLOCKED";
    if (activities.stream()
        .anyMatch(activity -> "IN_PROGRESS".equals(activity.operationalState()))) {
      return "IN_PROGRESS";
    }
    if (completed > 0) return "IN_PROGRESS";
    if (activities.stream().anyMatch(activity -> "PENDING".equals(activity.operationalState()))) {
      return "PENDING";
    }
    if (activities.stream().allMatch(activity -> "CANCELLED".equals(activity.operationalState()))) {
      return "CANCELLED";
    }
    return "NOT_STARTED";
  }

  /** Prioriza bloqueio, execução e pendência antes das atividades ainda sem registro. */
  private ProductProcessActivityExecutionGroupResponse currentActivity(
      List<ProductProcessActivityExecutionGroupResponse> activities) {
    for (String state : List.of("BLOCKED", "IN_PROGRESS", "PENDING", "NOT_STARTED", "CANCELLED")) {
      Optional<ProductProcessActivityExecutionGroupResponse> matching =
          activities.stream()
              .filter(activity -> state.equals(activity.operationalState()))
              .findFirst();
      if (matching.isPresent()) return matching.get();
    }
    return activities.stream()
        .filter(activity -> !activity.objectiveAchieved())
        .findFirst()
        .orElse(null);
  }

  /** Representa a situação auditável de uma atividade antes de montar o contrato público. */
  private record ActivitySituation(
      String operationalState,
      String stateReason,
      boolean objectiveAchieved,
      String stateEvidence,
      Long activityInstanceId,
      Integer occurrenceNumber) {}

  /** Representa o resumo gerencial da versão de processo vinculada ao produto. */
  private record ProductProcessSituation(
      String operationalState,
      boolean objectiveAchieved,
      int selectedActivityCount,
      int completedActivityCount,
      int remainingActivityCount,
      int blockedActivityCount,
      ProductProcessActivityExecutionGroupResponse currentActivity) {}

  /** Representa uma referência operacional candidata com sua ordem de criação auditável. */
  private record ExecutionReferenceCandidate(String sourceReference, Instant createdAt, Long id) {}

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
        task.getExecutionMode(),
        executionReasoningEffort(task, technicalExecution.orElse(null)),
        knownProductInternalName,
        firstPresent(
            task.getExecutionPrompt(),
            technicalExecution.map(GeraLandingStageExecution::getPrompt).orElse(null)),
        task.getExecutionAgentPrompt(),
        task.getExecutionActivityPrompt(),
        AgentTaskAuditView.blockerGuidance(task),
        AgentTaskAuditView.accessedUrls(task),
        AgentTaskAuditView.visualEvidence(task),
        AgentTaskResultView.section(task, "visualAudit"),
        AgentTaskResultView.section(task, "purchaseEmotion"));
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

  /** Resolve o produto quando a origem aponta para plano comercial ou experimento canônico. */
  private String productInternalName(AgentTask task) {
    if (task.getSourceReference() == null) return null;
    Matcher planMatcher = COMMERCIAL_PLAN_REFERENCE.matcher(task.getSourceReference());
    Matcher experimentMatcher = EXPERIMENT_REFERENCE.matcher(task.getSourceReference());
    try {
      if (planMatcher.matches() && commercialPlanRepository != null) {
        return commercialPlanRepository
            .findById(Long.valueOf(planMatcher.group(1)))
            .flatMap(this::internalProductName)
            .orElse(null);
      }
      if (experimentMatcher.matches() && experimentRepository != null) {
        return experimentRepository
            .findById(Long.valueOf(experimentMatcher.group(1)))
            .map(Experiment::getProduct)
            .map(Product::getInternalName)
            .filter(value -> value != null && !value.isBlank())
            .orElse(null);
      }
      return null;
    } catch (NumberFormatException ex) {
      LOGGER.warn(
          "Referência operacional inválida ao consultar produto da tarefa {}.", task.getId(), ex);
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
