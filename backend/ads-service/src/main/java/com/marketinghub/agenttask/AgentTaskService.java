package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocessresource.BusinessProcessExecutionResource;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
import com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: coordenar a caixa de entrada e o ciclo de vida das tarefas dos agentes. */
@Service
public class AgentTaskService {
  private static final Logger log = LoggerFactory.getLogger(AgentTaskService.class);
  private static final Set<String> ALLOWED_TRANSITIONS =
      Set.of(
          "PENDING:IN_PROGRESS",
          "PENDING:CANCELLED",
          "IN_PROGRESS:COMPLETED",
          "IN_PROGRESS:BLOCKED",
          "IN_PROGRESS:CANCELLED",
          "BLOCKED:IN_PROGRESS",
          "BLOCKED:CANCELLED");
  private static final Set<String> EXECUTION_MODES =
      Set.of("MODEL", "DETERMINISTIC", "NOT_STARTED");
  private static final Set<String> BLOCKER_CATEGORIES =
      Set.of(
          "FUNCTIONAL_ADJUSTMENT",
          "MISSING_EVIDENCE",
          "COMMERCIAL_RISK",
          "AUTHORIZATION_REQUIRED",
          "TECHNICAL_FAILURE");
  private static final Set<String> ACCESS_METHODS =
      Set.of("WEB_SEARCH", "BROWSER", "PLAYWRIGHT", "PUBLIC_API", "HTTP", "HTTP_CLIENT", "MCP");
  private static final Set<String> SENSITIVE_QUERY_PARAMETERS =
      Set.of(
          "accesstoken",
          "apikey",
          "auth",
          "authorization",
          "code",
          "credential",
          "clientsecret",
          "idtoken",
          "jwt",
          "key",
          "password",
          "refreshtoken",
          "secret",
          "session",
          "sessionid",
          "sig",
          "signature",
          "token");
  private static final String ACCESSED_URL_LINK = "ACCESSED_URL";
  private static final String BLOCKER_HELP_LINK = "BLOCKER_HELP";
  private static final Pattern PROMPT_PHASE_HEADER =
      Pattern.compile("(?m)^--- ([^\\r\\n]+) ---\\r?$");

  private final AgentTaskRepository repository;
  private final BusinessProcessActivityInstanceRepository activityInstanceRepository;
  private final AgentRepository agentRepository;
  private final BusinessProcessDefinitionRepository processRepository;
  private final BusinessProcessActivityDefinitionRepository activityDefinitionRepository;
  private final BusinessProcessExecutionResourceRepository executionResourceRepository;
  private final ObjectMapper objectMapper;
  private final OpenAiPricingService pricingService;
  private final Clock clock;
  private final MarketStrategicContextProvider marketStrategicContextProvider;
  private final AgentTaskTargetContextProvider taskTargetContextProvider;

  @Autowired(required = false)
  private ResearchIntelligenceService researchIntelligenceService;

  @Autowired(required = false)
  private CommunicationMaterializationContextProvider communicationMaterializationContextProvider =
      CommunicationMaterializationContextProvider.empty();

  @Autowired(required = false)
  private List<AgentTaskCompletionHook> completionHooks = List.of();

  /** Configura persistência, catálogo e relógio operacional. */
  @Autowired
  public AgentTaskService(
      AgentTaskRepository repository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      AgentRepository agentRepository,
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      BusinessProcessExecutionResourceRepository executionResourceRepository,
      ObjectMapper objectMapper,
      OpenAiPricingService pricingService,
      MarketStrategicContextProvider marketStrategicContextProvider,
      AgentTaskTargetContextProvider taskTargetContextProvider) {
    this(
        repository,
        activityInstanceRepository,
        agentRepository,
        processRepository,
        activityDefinitionRepository,
        executionResourceRepository,
        objectMapper,
        pricingService,
        Clock.systemUTC(),
        marketStrategicContextProvider,
        taskTargetContextProvider);
  }

  /** Permite testes determinísticos do histórico temporal. */
  AgentTaskService(
      AgentTaskRepository repository,
      AgentRepository agentRepository,
      BusinessProcessDefinitionRepository processRepository,
      ObjectMapper objectMapper,
      Clock clock) {
    this(
        repository,
        null,
        agentRepository,
        processRepository,
        null,
        null,
        objectMapper,
        null,
        clock,
        MarketStrategicContextProvider.empty());
  }

  /** Permite testes determinísticos do custo calculado pelo catálogo. */
  AgentTaskService(
      AgentTaskRepository repository,
      AgentRepository agentRepository,
      BusinessProcessDefinitionRepository processRepository,
      ObjectMapper objectMapper,
      OpenAiPricingService pricingService,
      Clock clock) {
    this(
        repository,
        null,
        agentRepository,
        processRepository,
        null,
        null,
        objectMapper,
        pricingService,
        clock,
        MarketStrategicContextProvider.empty());
  }

  /** Permite testar recursos especializados e custo com todas as fontes de verdade explícitas. */
  AgentTaskService(
      AgentTaskRepository repository,
      AgentRepository agentRepository,
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessExecutionResourceRepository executionResourceRepository,
      ObjectMapper objectMapper,
      OpenAiPricingService pricingService,
      Clock clock) {
    this(
        repository,
        null,
        agentRepository,
        processRepository,
        null,
        executionResourceRepository,
        objectMapper,
        pricingService,
        clock,
        MarketStrategicContextProvider.empty());
  }

  /** Permite testar o vínculo explícito entre atividade, instância e tarefas. */
  AgentTaskService(
      AgentTaskRepository repository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      AgentRepository agentRepository,
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      BusinessProcessExecutionResourceRepository executionResourceRepository,
      ObjectMapper objectMapper,
      OpenAiPricingService pricingService,
      Clock clock) {
    this(
        repository,
        activityInstanceRepository,
        agentRepository,
        processRepository,
        activityDefinitionRepository,
        executionResourceRepository,
        objectMapper,
        pricingService,
        clock,
        MarketStrategicContextProvider.empty());
  }

  /** Permite testar o contexto estratégico com todas as fontes explicitamente controladas. */
  AgentTaskService(
      AgentTaskRepository repository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      AgentRepository agentRepository,
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      BusinessProcessExecutionResourceRepository executionResourceRepository,
      ObjectMapper objectMapper,
      OpenAiPricingService pricingService,
      Clock clock,
      MarketStrategicContextProvider marketStrategicContextProvider) {
    this(
        repository,
        activityInstanceRepository,
        agentRepository,
        processRepository,
        activityDefinitionRepository,
        executionResourceRepository,
        objectMapper,
        pricingService,
        clock,
        marketStrategicContextProvider,
        AgentTaskTargetContextProvider.empty());
  }

  /** Permite testar contexto estratégico e alvo comercial com fontes controladas. */
  AgentTaskService(
      AgentTaskRepository repository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      AgentRepository agentRepository,
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      BusinessProcessExecutionResourceRepository executionResourceRepository,
      ObjectMapper objectMapper,
      OpenAiPricingService pricingService,
      Clock clock,
      MarketStrategicContextProvider marketStrategicContextProvider,
      AgentTaskTargetContextProvider taskTargetContextProvider) {
    this.repository = repository;
    this.activityInstanceRepository = activityInstanceRepository;
    this.agentRepository = agentRepository;
    this.processRepository = processRepository;
    this.activityDefinitionRepository = activityDefinitionRepository;
    this.executionResourceRepository = executionResourceRepository;
    this.objectMapper = objectMapper;
    this.pricingService = pricingService;
    this.clock = clock;
    this.marketStrategicContextProvider = marketStrategicContextProvider;
    this.taskTargetContextProvider = taskTargetContextProvider;
  }

  /** Abre uma solicitação humana na caixa do agente informado. */
  @Transactional
  public AgentTaskResponse createByHuman(CreateAgentTaskRequest request) {
    Agent assignee = agent(request.assignedAgentKey());
    ProcessBinding binding = validateProcessBinding(request, assignee);
    return save(
        assignee,
        null,
        "HUMAN",
        request.requestedByName(),
        request.title(),
        request.description(),
        request.priority(),
        request.sourceReference(),
        binding);
  }

  /**
   * Reutiliza a atividade humana já aberta para a mesma execução e impede delegações duplicadas.
   */
  @Transactional
  public AgentTaskResponse createByHumanIfAbsent(CreateAgentTaskRequest request) {
    String sourceReference = trimToNull(request.sourceReference());
    if (sourceReference == null) {
      throw new IllegalArgumentException("Tarefa idempotente exige referência de origem.");
    }
    return repository.findBySourceReferenceOrderByCreatedAtAscIdAsc(sourceReference).stream()
        .filter(
            task -> task.getAssignedAgent().getAgentKey().equals(request.assignedAgentKey().trim()))
        .filter(
            task ->
                task.getProcessDefinition() != null
                    && task.getProcessDefinition().getId().equals(request.processDefinitionId()))
        .filter(task -> Objects.equals(task.getProcessActivityId(), request.processActivityId()))
        .filter(task -> !"CANCELLED".equals(task.getStatus()))
        .findFirst()
        .map(this::response)
        .orElseGet(() -> createByHuman(request));
  }

  /**
   * Reutiliza a mesma execução em versões anteriores do processo quando as atividades são aliases
   * compatíveis.
   */
  @Transactional
  public AgentTaskResponse createByHumanIfAbsentAcrossProcessVersions(
      CreateAgentTaskRequest request, List<String> compatibleActivityIds) {
    String sourceReference = trimToNull(request.sourceReference());
    if (sourceReference == null) {
      throw new IllegalArgumentException("Tarefa idempotente exige referência de origem.");
    }
    Agent assignee = agent(request.assignedAgentKey());
    ProcessBinding binding = validateProcessBinding(request, assignee);
    Set<String> normalizedActivityIds =
        compatibleActivityIds == null
            ? Set.of()
            : compatibleActivityIds.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    if (binding.definition() == null || !normalizedActivityIds.contains(binding.activityId())) {
      throw new IllegalArgumentException(
          "Tarefa entre versões exige uma atividade regular incluída nos aliases compatíveis.");
    }
    String processCode = binding.definition().getProcessCode();
    return repository.findBySourceReferenceOrderByCreatedAtAscIdAsc(sourceReference).stream()
        .filter(task -> task.getAssignedAgent().getAgentKey().equals(assignee.getAgentKey()))
        .filter(
            task ->
                task.getProcessDefinition() != null
                    && processCode.equals(task.getProcessDefinition().getProcessCode()))
        .filter(task -> normalizedActivityIds.contains(task.getProcessActivityId()))
        .filter(task -> !"CANCELLED".equals(task.getStatus()))
        .findFirst()
        .map(this::response)
        .orElseGet(
            () ->
                save(
                    assignee,
                    null,
                    "HUMAN",
                    request.requestedByName(),
                    request.title(),
                    request.description(),
                    request.priority(),
                    request.sourceReference(),
                    binding));
  }

  /** Abre nova tentativa após bloqueio e atualiza o contexto de trabalho ainda não reservado. */
  @Transactional
  public AgentTaskResponse retryBlockedByHumanOrRefreshPending(CreateAgentTaskRequest request) {
    String sourceReference = trimToNull(request.sourceReference());
    if (sourceReference == null) {
      throw new IllegalArgumentException("Retentativa idempotente exige referência de origem.");
    }
    Optional<AgentTask> latest =
        repository.findBySourceReferenceOrderByCreatedAtAscIdAsc(sourceReference).stream()
            .filter(
                task ->
                    task.getAssignedAgent().getAgentKey().equals(request.assignedAgentKey().trim()))
            .filter(
                task ->
                    task.getProcessDefinition() != null
                        && task.getProcessDefinition()
                            .getId()
                            .equals(request.processDefinitionId()))
            .filter(
                task -> Objects.equals(task.getProcessActivityId(), request.processActivityId()))
            .filter(task -> !"CANCELLED".equals(task.getStatus()))
            .max(
                java.util.Comparator.comparing(
                    AgentTask::getId, java.util.Comparator.nullsFirst(Long::compareTo)));
    if (latest.isEmpty() || "BLOCKED".equals(latest.get().getStatus())) {
      return createByHuman(request);
    }
    AgentTask task = latest.get();
    if ("PENDING".equals(task.getStatus())) {
      task.setTitle(request.title().trim());
      task.setDescription(request.description().trim());
      task.setPriority(request.priority());
      task.setUpdatedAt(Instant.now(clock));
      task = repository.save(task);
    }
    return response(task);
  }

  /** Importa uma execução já validada, preservando resultado, evidência e consumo reais. */
  @Transactional
  public AgentTaskResponse recordImportedCompletedTask(ImportedCompletedAgentTask importedTask) {
    requireValidJson(importedTask.resultJson(), "resultado");
    requireValidJson(importedTask.evidenceJson(), "evidência");
    CreateAgentTaskRequest request =
        new CreateAgentTaskRequest(
            importedTask.assignedAgentKey(),
            importedTask.requestedByName(),
            importedTask.title(),
            importedTask.description(),
            "HIGH",
            importedTask.sourceReference(),
            importedTask.processDefinitionId(),
            importedTask.processActivityId(),
            false,
            null);
    Agent assignee = agent(request.assignedAgentKey());
    ProcessBinding binding = validateProcessBinding(request, assignee);
    AgentTaskResponse created =
        save(
            assignee,
            null,
            "HUMAN",
            request.requestedByName(),
            request.title(),
            request.description(),
            request.priority(),
            request.sourceReference(),
            binding);
    AgentTask task = repository.findById(created.id()).orElseThrow();
    Instant now = Instant.now(clock);
    task.setResultJson(importedTask.resultJson());
    task.setEvidenceJson(importedTask.evidenceJson());
    applyExecutionAudit(task, importedTask.executionAudit());
    applyModelUsage(task, importedTask.modelUsages());
    requireTerminalExecutionAudit(task, true);
    task.setStatus("COMPLETED");
    task.setReceivedAt(now);
    task.setDeliveredAt(now);
    task.setUpdatedAt(now);
    AgentTask saved = repository.save(task);
    synchronizeActivityInstance(saved, now);
    return response(saved);
  }

  /** Abre uma delegação entre agentes com instância regular ou excepcionalidade explícita. */
  @Transactional
  public AgentTaskResponse createByAgent(CreateAgentTaskByAgentRequest request) {
    Agent requester = agent(request.requestedByAgentKey());
    Agent assignee = agent(request.assignedAgentKey());
    ProcessBinding binding = validateProcessBinding(delegationBindingRequest(request), assignee);
    return save(
        assignee,
        requester,
        "AGENT",
        requester.getNickname(),
        request.title(),
        request.description(),
        request.priority(),
        request.sourceReference(),
        binding);
  }

  /**
   * Converte a delegação para o mesmo contrato governado usado pelas tarefas abertas por pessoas.
   */
  private CreateAgentTaskRequest delegationBindingRequest(CreateAgentTaskByAgentRequest request) {
    boolean hasProcess =
        request.processDefinitionId() != null || trimToNull(request.processActivityId()) != null;
    boolean exceptional = request.exceptional() || !hasProcess;
    String exceptionReason =
        exceptional
            ? Optional.ofNullable(trimToNull(request.exceptionReason()))
                .orElse(
                    "Delegação entre agentes sem atividade BPM informada pelo contrato de origem.")
            : null;
    return new CreateAgentTaskRequest(
        request.assignedAgentKey(),
        request.requestedByAgentKey(),
        request.title(),
        request.description(),
        request.priority(),
        request.sourceReference(),
        request.processDefinitionId(),
        request.processActivityId(),
        exceptional,
        exceptionReason);
  }

  /**
   * Abre uma única delegação operacional para uma causa persistida, mesmo sob callbacks repetidos.
   */
  @Transactional
  public AgentTaskResponse createOperationalDelegationIfAbsent(
      CreateAgentTaskByAgentRequest request) {
    String sourceReference = trimToNull(request.sourceReference());
    if (sourceReference == null) {
      throw new IllegalArgumentException("Delegação operacional exige referência de origem.");
    }
    Long requestedProcessId = request.processDefinitionId();
    String requestedActivityId = trimToNull(request.processActivityId());
    return repository.findBySourceReferenceOrderByCreatedAtAscIdAsc(sourceReference).stream()
        .filter(
            task -> request.assignedAgentKey().trim().equals(task.getAssignedAgent().getAgentKey()))
        .filter(
            task ->
                Objects.equals(
                    requestedProcessId,
                    task.getProcessDefinition() == null
                        ? null
                        : task.getProcessDefinition().getId()))
        .filter(task -> Objects.equals(requestedActivityId, task.getProcessActivityId()))
        .filter(task -> !"CANCELLED".equals(task.getStatus()))
        .max(
            java.util.Comparator.comparing(
                AgentTask::getId, java.util.Comparator.nullsFirst(Long::compareTo)))
        .map(this::response)
        .orElseGet(() -> createByAgent(request));
  }

  /** Sincroniza a delegação operacional com resultado, consumo e auditoria do executor. */
  @Transactional
  public void finishOperationalDelegation(
      String assignedAgentKey,
      String sourceReference,
      boolean successful,
      AgentTaskExecutionAuditRequest executionAudit,
      List<AgentTaskModelUsageRequest> modelUsages,
      String error) {
    repository
        .findTopByAssignedAgentAgentKeyAndSourceReferenceOrderByUpdatedAtDescIdDesc(
            assignedAgentKey, sourceReference)
        .ifPresent(
            task -> {
              if (List.of("COMPLETED", "CANCELLED").contains(task.getStatus())) return;
              Instant now = Instant.now(clock);
              applyExecutionAudit(task, executionAudit);
              applyModelUsage(task, modelUsages);
              if (successful) {
                requireTerminalExecutionAudit(task, true);
              } else {
                ensurePreModelFailureAudit(task, null, modelUsages);
                applyBlockerGuidance(
                    task,
                    null,
                    trimToNull(error) == null
                        ? "O executor responsável informou que a delegação não foi concluída."
                        : error,
                    null);
                requireTerminalExecutionAudit(task, false);
              }
              task.setStatus(successful ? "COMPLETED" : "BLOCKED");
              if (successful && task.getDeliveredAt() == null) {
                task.setDeliveredAt(now);
              }
              task.setUpdatedAt(now);
              AgentTask saved = repository.save(task);
              synchronizeActivityInstance(saved, now);
            });
  }

  /**
   * Abre na mesa do responsável uma decisão de gate que não pode ser concluída como tarefa comum.
   */
  @Transactional
  public AgentTaskResponse createGateByAgent(
      CreateAgentTaskByAgentRequest request, String gateCode) {
    AgentTaskResponse created = createByAgent(request);
    AgentTask task = repository.findById(created.id()).orElseThrow();
    task.setTaskKind("GATE_DECISION");
    task.setGateCode(gateCode.trim());
    task.setGateStatus("PENDING");
    AgentTask saved = repository.save(task);
    synchronizeActivityInstance(saved, Instant.now(clock));
    return response(saved);
  }

  /** Persiste a decisão do gate somente quando tomada pelo agente destinatário da tarefa. */
  @Transactional
  public AgentTaskResponse decideGate(Long taskId, DecideAgentGateRequest request) {
    AgentTask task = task(taskId);
    if (!"GATE_DECISION".equals(task.getTaskKind())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A tarefa não representa um gate.");
    }
    if (!task.getAssignedAgent().getAgentKey().equals(request.decidedByAgentKey().trim())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Somente o agente responsável pode decidir este gate.");
    }
    if (!"PENDING".equals(task.getGateStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "O gate já recebeu uma decisão.");
    }
    Instant now = Instant.now(clock);
    ensureDeterministicCommandAudit(
        task,
        "agent-gate-decision-v1",
        Map.of(
            "taskId", task.getId(),
            "gateCode", task.getGateCode(),
            "decision", request.decision(),
            "reason", request.reason().trim(),
            "decidedByAgentKey", request.decidedByAgentKey().trim()));
    if (!"APPROVED".equals(request.decision())) {
      applyBlockerGuidance(task, null, request.reason(), null);
      requireTerminalExecutionAudit(task, false);
    } else {
      requireTerminalExecutionAudit(task, true);
    }
    task.setGateStatus(request.decision());
    task.setGateDecisionReason(request.reason().trim());
    task.setGateDecidedAt(now);
    task.setStatus("APPROVED".equals(request.decision()) ? "COMPLETED" : "BLOCKED");
    if ("APPROVED".equals(request.decision()) && task.getDeliveredAt() == null) {
      task.setDeliveredAt(now);
    }
    task.setUpdatedAt(now);
    AgentTask saved = repository.save(task);
    synchronizeActivityInstance(saved, now);
    return response(saved);
  }

  /** Lista exclusivamente as tarefas destinadas ao agente solicitado. */
  @Transactional(readOnly = true)
  public List<AgentTaskResponse> inbox(String agentKey) {
    agent(agentKey);
    return repository
        .findByAssignedAgentAgentKeyOrderByCreatedAtDescIdDesc(agentKey.trim())
        .stream()
        .map(this::response)
        .toList();
  }

  /** Lista a fila operacional central com todo trabalho pendente, ativo ou bloqueado. */
  @Transactional(readOnly = true)
  public List<AgentTaskResponse> activeTasks() {
    return repository
        .findByStatusInOrderByUpdatedAtDescIdDesc(List.of("IN_PROGRESS", "BLOCKED", "PENDING"))
        .stream()
        .map(this::response)
        .toList();
  }

  /** Monta as instâncias BPM de uma entidade com liberação, bloqueios e legado substituído. */
  @Transactional(readOnly = true)
  public List<ProcessInstanceResponse> processInstances(String sourceReference) {
    String reference = trimToNull(sourceReference);
    if (reference == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe a referência da entidade.");
    }
    List<AgentTask> history = repository.findBySourceReferenceOrderByCreatedAtAscIdAsc(reference);
    Map<Long, List<AgentTask>> grouped = new java.util.LinkedHashMap<>();
    history.stream()
        .filter(task -> task.getProcessDefinition() != null)
        .forEach(
            task ->
                grouped
                    .computeIfAbsent(
                        task.getProcessDefinition().getId(), ignored -> new ArrayList<>())
                    .add(task));
    List<AgentTask> legacy =
        history.stream().filter(task -> task.getProcessDefinition() == null).toList();
    return grouped.values().stream()
        .map(tasks -> processInstance(reference, tasks, legacy))
        .toList();
  }

  /** Monta uma instância de processo preservando atividade, ocorrência e tentativa separadas. */
  private ProcessInstanceResponse processInstance(
      String sourceReference, List<AgentTask> tasks, List<AgentTask> legacy) {
    BusinessProcessDefinition process = tasks.get(0).getProcessDefinition();
    List<ProcessInstanceTaskResponse> items = processInstanceTasks(tasks, false);
    List<ProcessInstanceTaskResponse> superseded = processInstanceTasks(legacy, true);
    return new ProcessInstanceResponse(
        process.getId(),
        process.getProcessCode(),
        process.getVersionNumber(),
        sourceReference,
        processInstanceActivities(tasks),
        items,
        superseded);
  }

  /** Agrupa tentativas pela ocorrência persistida, com fallback legível para registros legados. */
  private List<ProcessInstanceActivityResponse> processInstanceActivities(List<AgentTask> tasks) {
    Map<String, List<AgentTask>> grouped = new java.util.LinkedHashMap<>();
    for (AgentTask task : tasks) {
      String key =
          task.getActivityInstance() == null
              ? "legacy:" + task.getProcessActivityId()
              : "instance:" + task.getActivityInstance().getId();
      grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(task);
    }
    return grouped.values().stream().map(this::processInstanceActivity).toList();
  }

  /** Converte uma ocorrência e suas tentativas no contrato gerencial da tela. */
  private ProcessInstanceActivityResponse processInstanceActivity(List<AgentTask> attempts) {
    AgentTask representative = attempts.get(attempts.size() - 1);
    BusinessProcessActivityInstance instance = representative.getActivityInstance();
    String status = instance == null ? aggregateStatus(attempts) : instance.getStatus();
    String operationalState = activityOperationalState(status, attempts);
    String stateReason = activityStateReason(operationalState, instance);
    BigDecimal fallbackCost =
        attempts.stream()
            .map(AgentTask::getEstimatedCostUsd)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO.setScale(8), BigDecimal::add);
    boolean fallbackHasCost =
        attempts.stream().anyMatch(task -> task.getEstimatedCostUsd() != null);
    boolean fallbackCompleteCoverage =
        attempts.stream()
            .allMatch(
                task ->
                    "ESTIMATED".equals(task.getCostEstimationStatus())
                        || "NOT_APPLICABLE".equals(task.getCostEstimationStatus()));
    BusinessProcessActivityDefinition definition =
        instance == null ? null : instance.getActivityDefinition();
    return new ProcessInstanceActivityResponse(
        instance == null ? null : instance.getId(),
        definition == null ? null : definition.getId(),
        representative.getProcessActivityId(),
        representative.getProcessActivityName() == null
            ? representative.getTitle()
            : representative.getProcessActivityName(),
        definition == null ? null : definition.getObjective(),
        instance == null ? 1 : instance.getOccurrenceNumber(),
        status,
        operationalState,
        stateReason,
        instance == null
            ? attempts.stream()
                .map(AgentTask::getCreatedAt)
                .filter(Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(null)
            : instance.getEnteredAt(),
        instance == null ? null : instance.getExitedAt(),
        instance != null && instance.isObjectiveAchieved(),
        instance == null
            ? (fallbackHasCost ? fallbackCost.setScale(8) : null)
            : instance.getKnownCostUsd(),
        instance == null
            ? (fallbackCompleteCoverage ? "COMPLETE" : fallbackHasCost ? "PARTIAL" : "NOT_REPORTED")
            : instance.getCostCoverage(),
        instance == null ? "LEGACY_DERIVED" : instance.getEvidenceQuality(),
        processInstanceTasks(attempts, false));
  }

  /** Consolida o estado das tentativas quando uma linha histórica ainda não possui instância. */
  private String aggregateStatus(List<AgentTask> attempts) {
    List<AgentTask> currentAttempts = currentAttemptsByAgent(attempts);
    if (currentAttempts.stream().anyMatch(task -> "IN_PROGRESS".equals(task.getStatus()))) {
      return "IN_PROGRESS";
    }
    if (currentAttempts.stream().anyMatch(task -> "BLOCKED".equals(task.getStatus()))) {
      return "BLOCKED";
    }
    if (currentAttempts.stream().anyMatch(task -> "PENDING".equals(task.getStatus()))) {
      return "PENDING";
    }
    if (!currentAttempts.isEmpty()
        && currentAttempts.stream().allMatch(task -> "COMPLETED".equals(task.getStatus()))) {
      return "COMPLETED";
    }
    return "CANCELLED";
  }

  /** Mantém a tentativa mais recente de cada responsável para calcular o estado funcional. */
  private List<AgentTask> currentAttemptsByAgent(List<AgentTask> attempts) {
    Map<String, AgentTask> latestAttemptByAgent = new java.util.LinkedHashMap<>();
    attempts.forEach(task -> latestAttemptByAgent.put(task.getAssignedAgent().getAgentKey(), task));
    return List.copyOf(latestAttemptByAgent.values());
  }

  /** Traduz o estado persistido da instância e a elegibilidade do grafo. */
  private String activityOperationalState(String status, List<AgentTask> attempts) {
    if (!"PENDING".equals(status)) return status;
    return attempts.stream()
            .filter(task -> "PENDING".equals(task.getStatus()))
            .anyMatch(this::predecessorsCompleted)
        ? "RELEASED"
        : "WAITING_PREDECESSOR";
  }

  /** Explica a situação da ocorrência sem obrigar o usuário a interpretar as tentativas. */
  private String activityStateReason(
      String operationalState, BusinessProcessActivityInstance instance) {
    return switch (operationalState) {
      case "RELEASED" -> "Atividade liberada para consumo pelo executor responsável.";
      case "WAITING_PREDECESSOR" ->
          "Aguardando a conclusão das atividades predecessoras do processo.";
      case "IN_PROGRESS" -> "Atividade com uma ou mais tentativas em execução.";
      case "BLOCKED" ->
          instance == null || trimToNull(instance.getBlockedReason()) == null
              ? "Atividade bloqueada por uma tentativa ainda não resolvida."
              : instance.getBlockedReason();
      case "COMPLETED" -> "Objetivo da instância atingido pelas tentativas concluídas.";
      case "CANCELLED" -> "Instância encerrada sem atingir o objetivo.";
      default -> "Estado consolidado pelo backend.";
    };
  }

  /** Numera as tentativas dentro de sua instância sem usar o índice como identidade. */
  private List<ProcessInstanceTaskResponse> processInstanceTasks(
      List<AgentTask> tasks, boolean supersededLegacy) {
    List<ProcessInstanceTaskResponse> responses = new ArrayList<>();
    for (int index = 0; index < tasks.size(); index++) {
      responses.add(processInstanceTask(tasks.get(index), supersededLegacy, index + 1));
    }
    return List.copyOf(responses);
  }

  /** Traduz o status persistido e a elegibilidade do grafo em situação legível. */
  private ProcessInstanceTaskResponse processInstanceTask(
      AgentTask task, boolean supersededLegacy, int attemptNumber) {
    String state;
    String reason;
    if (supersededLegacy) {
      state = "SUPERSEDED_LEGACY";
      reason = "Tarefa legada substituída pela instância BPM vinculada à mesma entidade.";
    } else if ("PENDING".equals(task.getStatus()) && predecessorsCompleted(task)) {
      state = "RELEASED";
      reason = "Atividade liberada para consumo pelo executor responsável.";
    } else if ("PENDING".equals(task.getStatus())) {
      state = "WAITING_PREDECESSOR";
      reason = "Aguardando a conclusão das atividades predecessoras do processo.";
    } else if ("IN_PROGRESS".equals(task.getStatus())) {
      state = "IN_PROGRESS";
      reason = "Atividade recebida e em execução pelo agente.";
    } else if ("BLOCKED".equals(task.getStatus())) {
      state = "BLOCKED";
      reason =
          task.getExecutionError() == null
              ? "Atividade bloqueada pelo executor."
              : task.getExecutionError();
    } else {
      state = task.getStatus();
      reason = "Estado final registrado na tarefa.";
    }
    return new ProcessInstanceTaskResponse(
        task.getId(),
        task.getActivityInstance() == null ? null : task.getActivityInstance().getId(),
        attemptNumber,
        task.getProcessActivityId(),
        task.getProcessActivityName() == null ? task.getTitle() : task.getProcessActivityName(),
        task.getAssignedAgent().getAgentKey(),
        task.getAssignedAgent().getNickname(),
        task.getStatus(),
        state,
        reason,
        failureAudit(task),
        task.getExecutionMode(),
        task.getExecutionModelCode(),
        task.getExecutionReasoningEffort(),
        task.getExecutionPrompt(),
        task.getExecutionAgentPrompt(),
        task.getExecutionActivityPrompt(),
        AgentTaskAuditView.blockerGuidance(task),
        AgentTaskAuditView.accessedUrls(task),
        AgentTaskAuditView.visualEvidence(task),
        AgentTaskResultView.section(task, "visualAudit"),
        AgentTaskResultView.section(task, "purchaseEmotion"),
        task.getInputTokens(),
        task.getCachedInputTokens(),
        task.getOutputTokens(),
        task.getEstimatedCostUsd(),
        task.getCostEstimationStatus(),
        task.getReceivedAt(),
        task.getDeliveredAt());
  }

  /** Atualiza o estado sem permitir saltos que eliminem a rastreabilidade do trabalho. */
  @Transactional
  public AgentTaskResponse updateStatus(Long taskId, UpdateAgentTaskStatusRequest request) {
    AgentTask task = task(taskId);
    String next = request.status();
    if ("GATE_DECISION".equals(task.getTaskKind()) && !"PENDING".equals(task.getGateStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Gate decidido não pode ser reaberto pelo status da tarefa.");
    }
    if ("GATE_DECISION".equals(task.getTaskKind())
        && ("COMPLETED".equals(next) || "CANCELLED".equals(next))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Gate deve ser encerrado pelo contrato de decisão.");
    }
    if (!task.getStatus().equals(next)
        && !ALLOWED_TRANSITIONS.contains(task.getStatus() + ":" + next)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Transição de status inválida.");
    }
    if ("COMPLETED".equals(next)) {
      ensureDeterministicCommandAudit(
          task,
          "agent-task-status-command-v1",
          Map.of(
              "taskId", task.getId(),
              "previousStatus", task.getStatus(),
              "requestedStatus", next,
              "sourceReference", Objects.toString(task.getSourceReference(), ""),
              "processActivityId", Objects.toString(task.getProcessActivityId(), "")));
      requireTerminalExecutionAudit(task, true);
    } else if ("BLOCKED".equals(next)) {
      ensurePreModelFailureAudit(task, null, null);
      applyBlockerGuidance(task, null, task.getExecutionError(), task.getResultJson());
      requireTerminalExecutionAudit(task, false);
    }
    Instant now = Instant.now(clock);
    task.setStatus(next);
    if ("IN_PROGRESS".equals(next) && task.getReceivedAt() == null) {
      task.setReceivedAt(now);
    }
    if ("COMPLETED".equals(next) && task.getDeliveredAt() == null) {
      task.setDeliveredAt(now);
    }
    task.setUpdatedAt(now);
    AgentTask saved = repository.save(task);
    synchronizeActivityInstance(saved, now);
    return response(saved);
  }

  /**
   * Cancela o trabalho ainda reabrível de uma entidade encerrada e preserva o diagnóstico já
   * registrado em cada tarefa.
   */
  @Transactional
  public int cancelActiveTasksBySourceReference(String sourceReference, String reason) {
    String reference = trimToNull(sourceReference);
    if (reference == null) {
      return 0;
    }
    Instant now = Instant.now(clock);
    String normalizedReason = trimToNull(reason);
    List<AgentTask> changed =
        repository.findBySourceReferenceOrderByCreatedAtAscIdAsc(reference).stream()
            .filter(task -> List.of("PENDING", "IN_PROGRESS", "BLOCKED").contains(task.getStatus()))
            .peek(
                task -> {
                  task.setStatus("CANCELLED");
                  if (normalizedReason != null
                      && !org.springframework.util.StringUtils.hasText(task.getExecutionError())) {
                    task.setExecutionError(normalizedReason);
                  }
                  task.setUpdatedAt(now);
                })
            .toList();
    repository.saveAll(changed);
    changed.stream()
        .filter(task -> task.getActivityInstance() != null)
        .collect(
            java.util.stream.Collectors.toMap(
                task -> task.getActivityInstance().getId(),
                task -> task,
                (first, ignored) -> first))
        .values()
        .forEach(task -> synchronizeActivityInstance(task, now));
    return changed.size();
  }

  /** Migra uma tarefa excepcional ainda não recebida para uma atividade BPM publicada. */
  @Transactional
  public AgentTaskResponse bindProcess(Long taskId, BindAgentTaskProcessRequest request) {
    AgentTask task = task(taskId);
    if (!task.isExceptional() || task.getProcessDefinition() != null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente tarefa excepcional sem processo pode ser vinculada.");
    }
    if (!"PENDING".equals(task.getStatus()) || task.getReceivedAt() != null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente tarefa pendente e ainda não recebida pode ser vinculada.");
    }
    CreateAgentTaskRequest bindingRequest =
        new CreateAgentTaskRequest(
            task.getAssignedAgent().getAgentKey(),
            task.getRequestedByName(),
            task.getTitle(),
            task.getDescription(),
            task.getPriority(),
            task.getSourceReference(),
            request.processDefinitionId(),
            request.processActivityId(),
            false,
            null);
    ProcessBinding binding = validateProcessBinding(bindingRequest, task.getAssignedAgent());
    Instant now = Instant.now(clock);
    task.setProcessDefinition(binding.definition());
    task.setProcessActivityId(binding.activityId());
    task.setProcessActivityName(binding.activityName());
    task.setActivityInstance(
        resolveActivityInstance(binding, task.getSourceReference(), task.getCreatedAt(), now));
    task.setExceptional(false);
    task.setExceptionReason(null);
    task.setUpdatedAt(now);
    AgentTask saved = repository.save(task);
    synchronizeActivityInstance(saved, now);
    return response(saved);
  }

  /** Persiste a tarefa normalizada com o primeiro estado auditável. */
  private AgentTaskResponse save(
      Agent assignee,
      Agent requester,
      String requesterType,
      String requesterName,
      String title,
      String description,
      String priority,
      String sourceReference,
      ProcessBinding binding) {
    Instant now = Instant.now(clock);
    AgentTask task = new AgentTask();
    task.setAssignedAgent(assignee);
    task.setRequestedByAgent(requester);
    task.setRequestedByType(requesterType);
    task.setRequestedByName(requesterName.trim());
    task.setTitle(title.trim());
    task.setDescription(description.trim());
    task.setPriority(priority);
    task.setStatus("PENDING");
    task.setSourceReference(trimToNull(sourceReference));
    task.setTaskKind("WORK");
    task.setProcessDefinition(binding == null ? null : binding.definition());
    task.setProcessActivityId(binding == null ? null : binding.activityId());
    task.setProcessActivityName(binding == null ? null : binding.activityName());
    task.setActivityInstance(resolveActivityInstance(binding, sourceReference, now, now));
    task.setExceptional(binding != null && binding.exceptional());
    task.setExceptionReason(binding == null ? null : binding.exceptionReason());
    task.setReceivedAt(null);
    task.setCreatedAt(now);
    task.setUpdatedAt(now);
    AgentTask saved = repository.save(task);
    synchronizeActivityInstance(saved, now);
    return response(saved);
  }

  /** Resolve ou abre a ocorrência que agrupa as tentativas da atividade para a mesma referência. */
  private BusinessProcessActivityInstance resolveActivityInstance(
      ProcessBinding binding, String sourceReference, Instant enteredAt, Instant now) {
    if (binding == null || binding.exceptional() || binding.activityDefinition() == null)
      return null;
    if (activityInstanceRepository == null) return null;
    String reference = trimToNull(sourceReference);
    if (reference == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Tarefa regular exige referência operacional para criar a instância da atividade.");
    }
    Optional<BusinessProcessActivityInstance> latest =
        activityInstanceRepository
            .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                binding.activityDefinition().getId(), reference);
    if (latest.isPresent() && !isTerminalActivityInstance(latest.get())) {
      return latest.get();
    }
    BusinessProcessActivityInstance instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(binding.activityDefinition());
    instance.setSourceReference(reference);
    instance.setOccurrenceNumber(latest.map(value -> value.getOccurrenceNumber() + 1).orElse(1));
    instance.setStatus("PENDING");
    instance.setEnteredAt(enteredAt);
    instance.setExitedAt(null);
    instance.setObjectiveAchieved(false);
    instance.setKnownCostUsd(null);
    instance.setCostCoverage("NOT_REPORTED");
    instance.setEvidenceQuality("DIRECT");
    instance.setCreatedAt(now);
    instance.setUpdatedAt(now);
    return activityInstanceRepository.save(instance);
  }

  /** Preserva ocorrências encerradas e abre uma nova quando a atividade for executada novamente. */
  private boolean isTerminalActivityInstance(BusinessProcessActivityInstance instance) {
    return "COMPLETED".equals(instance.getStatus()) || "CANCELLED".equals(instance.getStatus());
  }

  /**
   * Recalcula a verdade consolidada da instância a partir de todas as suas tentativas persistidas.
   */
  private void synchronizeActivityInstance(AgentTask changedTask, Instant now) {
    if (changedTask == null) return;
    BusinessProcessActivityInstance instance = changedTask.getActivityInstance();
    if (instance == null || activityInstanceRepository == null) return;
    List<AgentTask> persistedAttempts =
        repository.findByActivityInstanceIdOrderByCreatedAtAscIdAsc(instance.getId());
    List<AgentTask> attempts =
        persistedAttempts == null || persistedAttempts.isEmpty()
            ? List.of(changedTask)
            : persistedAttempts;
    List<AgentTask> currentAttempts = currentAttemptsByAgent(attempts);
    boolean inProgress =
        currentAttempts.stream().anyMatch(task -> "IN_PROGRESS".equals(task.getStatus()));
    boolean blocked = currentAttempts.stream().anyMatch(task -> "BLOCKED".equals(task.getStatus()));
    boolean pending = currentAttempts.stream().anyMatch(task -> "PENDING".equals(task.getStatus()));
    boolean completed =
        !currentAttempts.isEmpty()
            && currentAttempts.stream().allMatch(task -> "COMPLETED".equals(task.getStatus()));
    String status =
        inProgress
            ? "IN_PROGRESS"
            : blocked ? "BLOCKED" : pending ? "PENDING" : completed ? "COMPLETED" : "CANCELLED";
    instance.setStatus(status);
    instance.setEnteredAt(
        attempts.stream()
            .map(AgentTask::getCreatedAt)
            .filter(Objects::nonNull)
            .min(Instant::compareTo)
            .orElse(instance.getEnteredAt()));
    if ("COMPLETED".equals(status)) {
      instance.setObjectiveAchieved(true);
      instance.setExitedAt(
          currentAttempts.stream()
              .map(
                  task ->
                      task.getDeliveredAt() == null ? task.getUpdatedAt() : task.getDeliveredAt())
              .filter(Objects::nonNull)
              .max(Instant::compareTo)
              .orElse(now));
      instance.setObjectiveEvidenceJson(
          currentAttempts.stream()
              .filter(task -> "COMPLETED".equals(task.getStatus()))
              .map(task -> trimToNull(task.getEvidenceJson()))
              .filter(Objects::nonNull)
              .reduce((ignored, latest) -> latest)
              .orElse(null));
      instance.setBlockedReason(null);
    } else {
      instance.setObjectiveAchieved(false);
      instance.setExitedAt("CANCELLED".equals(status) ? now : null);
      instance.setObjectiveEvidenceJson(null);
      instance.setBlockedReason(
          currentAttempts.stream()
              .filter(task -> "BLOCKED".equals(task.getStatus()))
              .map(task -> trimToNull(task.getExecutionError()))
              .filter(Objects::nonNull)
              .reduce((ignored, latest) -> latest)
              .orElse(null));
    }
    BigDecimal knownCost =
        attempts.stream()
            .map(AgentTask::getEstimatedCostUsd)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO.setScale(8), BigDecimal::add);
    boolean hasKnownCost = attempts.stream().anyMatch(task -> task.getEstimatedCostUsd() != null);
    boolean completeCoverage =
        attempts.stream()
            .allMatch(
                task ->
                    "ESTIMATED".equals(task.getCostEstimationStatus())
                        || "NOT_APPLICABLE".equals(task.getCostEstimationStatus()));
    instance.setKnownCostUsd(hasKnownCost ? knownCost.setScale(8) : null);
    instance.setCostCoverage(
        completeCoverage ? "COMPLETE" : hasKnownCost ? "PARTIAL" : "NOT_REPORTED");
    if ("BACKFILLED_FROM_TASKS".equals(instance.getEvidenceQuality())) {
      instance.setEvidenceQuality("MIXED");
    }
    instance.setUpdatedAt(now);
    activityInstanceRepository.save(instance);
  }

  /** Resolve um agente ativo no catálogo pela identidade técnica estável. */
  private Agent agent(String agentKey) {
    return agentRepository
        .findByAgentKey(agentKey.trim())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agente não encontrado."));
  }

  /** Converte a persistência em contrato público sem expor entidades JPA. */
  private AgentTaskResponse response(AgentTask task) {
    Agent requester = task.getRequestedByAgent();
    Agent assignee = task.getAssignedAgent();
    return new AgentTaskResponse(
        task.getId(),
        assignee.getId(),
        assignee.getAgentKey(),
        assignee.getNickname(),
        task.getRequestedByType(),
        requester == null ? null : requester.getId(),
        requester == null ? null : requester.getAgentKey(),
        task.getRequestedByName(),
        task.getTitle(),
        task.getDescription(),
        task.getPriority(),
        task.getStatus(),
        task.getSourceReference(),
        task.getProcessDefinition() == null ? null : task.getProcessDefinition().getId(),
        task.getProcessDefinition() == null ? null : task.getProcessDefinition().getProcessCode(),
        task.getProcessDefinition() == null ? null : task.getProcessDefinition().getVersionNumber(),
        task.getProcessActivityId(),
        task.getProcessActivityName(),
        task.isExceptional(),
        task.getExceptionReason(),
        task.getTaskKind(),
        task.getGateCode(),
        task.getGateStatus(),
        task.getGateDecisionReason(),
        task.getGateDecidedAt(),
        task.getResultJson(),
        task.getEvidenceJson(),
        task.getExecutionError(),
        failureAudit(task),
        task.getInputTokens(),
        task.getCachedInputTokens(),
        task.getOutputTokens(),
        task.getEstimatedCostUsd(),
        task.getCostEstimationStatus(),
        task.getModelUsageUpdatedAt(),
        task.getExecutionModelCode(),
        task.getExecutionMode(),
        task.getExecutionReasoningEffort(),
        task.getExecutionPrompt(),
        task.getExecutionAgentPrompt(),
        task.getExecutionActivityPrompt(),
        AgentTaskAuditView.blockerGuidance(task),
        AgentTaskAuditView.accessedUrls(task),
        AgentTaskAuditView.visualEvidence(task),
        AgentTaskResultView.section(task, "visualAudit"),
        AgentTaskResultView.section(task, "purchaseEmotion"),
        task.getReceivedAt(),
        task.getDeliveredAt(),
        task.getCreatedAt(),
        task.getUpdatedAt());
  }

  /** Reconstrói intenção, contexto, acesso, saída e erro sem depender de logs técnicos. */
  private AgentTaskFailureAuditResponse failureAudit(AgentTask task) {
    if (!"BLOCKED".equals(task.getStatus())) return null;
    List<String> missing = new ArrayList<>();
    String intendedWork = trimToNull(task.getDescription());
    if (intendedWork == null) intendedWork = trimToNull(task.getTitle());
    String sourceReference = trimToNull(task.getSourceReference());
    String evidence = trimToNull(task.getEvidenceJson());
    String error = trimToNull(task.getExecutionError());
    if (error == null) error = trimToNull(task.getGateDecisionReason());
    BusinessProcessDefinition process = task.getProcessDefinition();
    String processCode = process == null ? null : trimToNull(process.getProcessCode());
    String activityId = trimToNull(task.getProcessActivityId());
    String authorityPolicy = trimToNull(task.getAssignedAgent().getAuthorityPolicy());
    if (intendedWork == null) missing.add("trabalho pretendido");
    if (sourceReference == null) missing.add("referência da entidade");
    if (processCode == null || activityId == null) missing.add("processo e atividade");
    if (authorityPolicy == null) missing.add("limite de autoridade");
    if (error == null) missing.add("causa da falha ou bloqueio");
    AgentTaskBlockerGuidanceResponse guidance = AgentTaskAuditView.blockerGuidance(task);
    if (guidance == null || guidance.helpLinks().isEmpty()) {
      missing.add("orientação acionável com link");
    }
    if (evidence == null) {
      missing.add("evidências acessadas");
    } else if (!isValidJson(task.getId(), evidence)) {
      missing.add("evidências em JSON válido");
    }
    return new AgentTaskFailureAuditResponse(
        missing.isEmpty() ? "COMPLETE" : "PARTIAL",
        intendedWork,
        sourceReference,
        processCode,
        activityId,
        task.getProcessActivityName(),
        authorityPolicy,
        evidence,
        trimToNull(task.getResultJson()),
        error,
        guidance,
        AgentTaskAuditView.accessedUrls(task),
        List.copyOf(missing));
  }

  /** Confirma que a evidência persistida pode ser lida de forma determinística. */
  private boolean isValidJson(Long taskId, String value) {
    try {
      objectMapper.readTree(value);
      return true;
    } catch (Exception ex) {
      log.warn(
          "Evidência inválida ao reconstruir o log governado da tarefa. taskId={}", taskId, ex);
      return false;
    }
  }

  /** Bloqueia a importação quando resultado ou evidência não são JSON auditável. */
  private void requireValidJson(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " obrigatória.");
    }
    try {
      objectMapper.readTree(value);
    } catch (Exception ex) {
      log.error("Falha ao validar JSON de execução importada. campo={}", label, ex);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, label + " deve ser um JSON válido.", ex);
    }
  }

  /** Resolve uma tarefa existente para mudanças auditáveis de estado ou gate. */
  private AgentTask task(Long taskId) {
    return repository
        .findById(taskId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada."));
  }

  /** Normaliza referências opcionais sem persistir texto vazio. */
  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** Valida a atividade publicada ou a justificativa obrigatória da tarefa excepcional. */
  private ProcessBinding validateProcessBinding(CreateAgentTaskRequest request, Agent assignee) {
    if (request.exceptional()) {
      String reason = trimToNull(request.exceptionReason());
      if (reason == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Tarefa excepcional exige justificativa.");
      }
      if (request.processDefinitionId() != null
          || trimToNull(request.processActivityId()) != null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Tarefa excepcional não pode apontar para atividade regular.");
      }
      return new ProcessBinding(null, null, null, "Atividade excepcional", true, reason);
    }
    if (request.processDefinitionId() == null || trimToNull(request.processActivityId()) == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Selecione um processo publicado e uma atividade.");
    }
    BusinessProcessDefinition definition =
        processRepository
            .findById(request.processDefinitionId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Processo não encontrado."));
    if (!"PUBLISHED".equals(definition.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A tarefa só pode usar uma versão publicada do processo.");
    }
    try {
      JsonNode nodes = objectMapper.readTree(definition.getDiagramJson()).path("nodes");
      for (JsonNode node : nodes) {
        if (request.processActivityId().trim().equals(node.path("id").asText())
            && "TASK".equals(node.path("type").asText())) {
          String owner = node.path("owner").asText("").trim();
          if (!activityAssigneeMatches(node, owner, assignee)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A atividade selecionada pertence a outro responsável.");
          }
          validateExecutionResourceAssignee(node, assignee);
          BusinessProcessActivityDefinition activityDefinition =
              resolveActivityDefinition(definition, node.path("id").asText());
          return new ProcessBinding(
              definition,
              activityDefinition,
              node.path("id").asText(),
              node.path("label").asText(),
              false,
              null);
        }
      }
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error(
          "Falha ao validar vínculo da tarefa ao processo. processDefinitionId={} activityId={} agentKey={}",
          request.processDefinitionId(),
          request.processActivityId(),
          assignee.getAgentKey(),
          ex);
      throw new IllegalStateException("Não foi possível validar o processo da tarefa.", ex);
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Atividade do processo não encontrada.");
  }

  /** Exige a identidade relacional da atividade quando a nova persistência está disponível. */
  private BusinessProcessActivityDefinition resolveActivityDefinition(
      BusinessProcessDefinition process, String activityId) {
    if (activityDefinitionRepository == null) return null;
    return activityDefinitionRepository
        .findByProcessDefinitionIdAndActivityId(process.getId(), activityId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A atividade publicada ainda não possui identidade operacional persistida."));
  }

  /** Reconhece o responsável do BPM pelo apelido, chave técnica ou nome funcional do agente. */
  private boolean activityOwnerMatchesAgent(String owner, Agent assignee) {
    String normalizedOwner = owner.toLowerCase(Locale.ROOT);
    String[] identities = {assignee.getNickname(), assignee.getAgentKey(), assignee.getName()};
    for (String identity : identities) {
      if (identity != null
          && !identity.isBlank()
          && normalizedOwner.contains(identity.toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
  }

  /** Prioriza as identidades técnicas dos coautores e mantém o texto legado como fallback. */
  private boolean activityAssigneeMatches(JsonNode node, String owner, Agent assignee) {
    JsonNode responsibleAgentKeys = node.path("responsibleAgentKeys");
    if (responsibleAgentKeys.isArray() && !responsibleAgentKeys.isEmpty()) {
      for (JsonNode responsibleAgentKey : responsibleAgentKeys) {
        if (assignee.getAgentKey().equals(responsibleAgentKey.asText())) return true;
      }
      return false;
    }
    return owner.isEmpty() || activityOwnerMatchesAgent(owner, assignee);
  }

  /** Impede vincular uma atividade especializada ao agente diferente do catálogo do recurso. */
  private void validateExecutionResourceAssignee(JsonNode node, Agent assignee) {
    String resourceCode = trimToNull(node.path("executionResourceCode").asText(null));
    if (resourceCode == null) return;
    BusinessProcessExecutionResource resource = requiredExecutionResource(resourceCode);
    if (!resource.getResponsibleAgentKey().equals(assignee.getAgentKey())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O recurso especializado pertence a outro agente responsável.");
    }
  }

  /** Mantém os dados validados do vínculo antes da persistência da tarefa. */
  private record ProcessBinding(
      BusinessProcessDefinition definition,
      BusinessProcessActivityDefinition activityDefinition,
      String activityId,
      String activityName,
      boolean exceptional,
      String exceptionReason) {}

  /** Reserva atomicamente a primeira atividade liberada pelo grafo do processo. */
  @Transactional
  public Optional<AgentTaskPendingResponse> claimEligibleProcessTask(String agentKey) {
    return claimEligibleProcessTask(agentKey, null, null, null);
  }

  /** Reserva somente a atividade suportada pelo executor especializado informado. */
  @Transactional
  public Optional<AgentTaskPendingResponse> claimEligibleProcessTask(
      String agentKey, String processCode, String activityId) {
    return claimEligibleProcessTask(agentKey, processCode, activityId, null);
  }

  /** Reserva somente trabalho compatível com processo, atividade e recurso deste executor. */
  @Transactional
  public Optional<AgentTaskPendingResponse> claimEligibleProcessTask(
      String agentKey, String processCode, String activityId, String executionResourceCode) {
    agent(agentKey);
    Optional<AgentTask> recovered =
        recoverInterruptedCallbackOnce(agentKey, processCode, activityId, executionResourceCode);
    if (recovered.isPresent()) return Optional.of(pendingResponse(recovered.get()));
    for (AgentTask task :
        repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            agentKey.trim(), "WORK", "PENDING")) {
      if (task.getProcessDefinition() == null
          || !matchesExecutionContract(task, processCode, activityId, executionResourceCode)
          || !predecessorsCompleted(task)) continue;
      Instant now = Instant.now(clock);
      task.setStatus("IN_PROGRESS");
      if (task.getReceivedAt() == null) task.setReceivedAt(now);
      task.setUpdatedAt(now);
      AgentTask saved = repository.save(task);
      synchronizeActivityInstance(saved, now);
      return Optional.of(pendingResponse(task));
    }
    return Optional.empty();
  }

  /** Reserva idempotentemente a tarefa exata já correlacionada por outro contrato do backend. */
  @Transactional
  public AgentTaskPendingResponse claimLinkedProcessTask(String agentKey, Long taskId) {
    agent(agentKey);
    AgentTask linkedTask = task(taskId);
    if (!linkedTask.getAssignedAgent().getAgentKey().equals(agentKey.trim())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tarefa pertence a outro agente.");
    }
    if (linkedTask.getProcessDefinition() == null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Tarefa correlacionada não pertence a um processo publicado.");
    }
    if ("IN_PROGRESS".equals(linkedTask.getStatus())) {
      if (linkedTask.getReceivedAt() == null) {
        Instant now = Instant.now(clock);
        linkedTask.setReceivedAt(now);
        linkedTask.setUpdatedAt(now);
        AgentTask saved = repository.save(linkedTask);
        synchronizeActivityInstance(saved, now);
        return pendingResponse(saved);
      }
      return pendingResponse(linkedTask);
    }
    if (!"PENDING".equals(linkedTask.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Tarefa correlacionada não está disponível para reserva.");
    }
    if (!predecessorsCompleted(linkedTask)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "As atividades predecessoras ainda não foram concluídas.");
    }
    Instant now = Instant.now(clock);
    linkedTask.setStatus("IN_PROGRESS");
    if (linkedTask.getReceivedAt() == null) linkedTask.setReceivedAt(now);
    linkedTask.setUpdatedAt(now);
    AgentTask saved = repository.save(linkedTask);
    synchronizeActivityInstance(saved, now);
    return pendingResponse(saved);
  }

  /**
   * Persiste a auditoria disponível de uma execução correlacionada sem inventar tier, custo ou
   * raciocínio ausente.
   */
  @Transactional
  public void recordClaimedProcessTaskExecutionAudit(
      String agentKey,
      Long taskId,
      String modelCode,
      String reasoningEffort,
      String promptSent,
      String agentPromptPart,
      String activityPromptPart,
      Long inputTokens,
      Long cachedInputTokens,
      Long outputTokens,
      Boolean modelInvocation) {
    AgentTask task = claimedBy(agentKey, taskId);
    String normalizedModel = trimToNull(modelCode);
    String normalizedReasoning = trimToNull(reasoningEffort);
    String normalizedPrompt = trimToNull(promptSent) == null ? null : promptSent;
    if (Boolean.TRUE.equals(modelInvocation)) {
      task.setExecutionMode("MODEL");
    } else if (Boolean.FALSE.equals(modelInvocation)) {
      task.setExecutionMode("DETERMINISTIC");
      normalizedReasoning = "NOT_APPLICABLE";
    }
    if (normalizedModel != null) task.setExecutionModelCode(normalizedModel);
    if (normalizedReasoning != null) task.setExecutionReasoningEffort(normalizedReasoning);
    if (normalizedPrompt != null) task.setExecutionPrompt(normalizedPrompt);
    if (Boolean.TRUE.equals(modelInvocation)) {
      task.setExecutionAgentPrompt(agentPromptPart);
      task.setExecutionActivityPrompt(activityPromptPart);
    } else if (Boolean.FALSE.equals(modelInvocation) && normalizedPrompt != null) {
      task.setExecutionAgentPrompt(null);
      task.setExecutionActivityPrompt(normalizedPrompt);
    }

    boolean anyTokenReported =
        inputTokens != null || cachedInputTokens != null || outputTokens != null;
    boolean everyTokenReported =
        inputTokens != null && cachedInputTokens != null && outputTokens != null;
    if (anyTokenReported && !everyTokenReported) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A auditoria exige todos os contadores de tokens.");
    }
    if (everyTokenReported
        && (inputTokens < 0
            || cachedInputTokens < 0
            || outputTokens < 0
            || cachedInputTokens > inputTokens)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Contadores de tokens inválidos na execução correlacionada.");
    }

    Instant now = Instant.now(clock);
    if (Boolean.FALSE.equals(modelInvocation)) {
      task.setInputTokens(0L);
      task.setCachedInputTokens(0L);
      task.setOutputTokens(0L);
      task.setEstimatedCostUsd(BigDecimal.ZERO.setScale(8));
      task.setCostEstimationStatus("NOT_APPLICABLE");
      task.setModelUsageUpdatedAt(now);
    } else if (everyTokenReported) {
      task.setInputTokens(inputTokens);
      task.setCachedInputTokens(cachedInputTokens);
      task.setOutputTokens(outputTokens);
      task.setEstimatedCostUsd(null);
      task.setCostEstimationStatus("PRICING_UNAVAILABLE");
      task.setModelUsageUpdatedAt(now);
    }
    if (modelInvocation != null) requireExecutionAuditShape(task, false);
    task.setUpdatedAt(now);
    AgentTask saved = repository.save(task);
    synchronizeActivityInstance(saved, now);
  }

  /** Persiste o envelope tipado antes ou depois da chamada sem encerrar a tarefa. */
  @Transactional
  public void recordClaimedProcessTaskExecutionAudit(
      String agentKey, Long taskId, AgentTaskExecutionAuditRequest audit) {
    AgentTask task = claimedBy(agentKey, taskId);
    applyExecutionAudit(task, audit);
    requireExecutionAuditShape(task, false);
    Instant now = Instant.now(clock);
    task.setUpdatedAt(now);
    AgentTask saved = repository.save(task);
    synchronizeActivityInstance(saved, now);
  }

  /** Impede que um prompt especializado reserve atividade de outro processo ou responsabilidade. */
  private boolean matchesExecutionContract(
      AgentTask task,
      String expectedProcessCode,
      String expectedActivityId,
      String expectedExecutionResourceCode) {
    String actualResourceCode = activityExecutionResourceCode(task);
    boolean resourceMatches =
        expectedExecutionResourceCode == null || expectedExecutionResourceCode.isBlank()
            ? actualResourceCode == null
            : expectedExecutionResourceCode.trim().equals(actualResourceCode);
    return (expectedProcessCode == null
            || expectedProcessCode.isBlank()
            || expectedProcessCode.trim().equals(task.getProcessDefinition().getProcessCode()))
        && (expectedActivityId == null
            || expectedActivityId.isBlank()
            || expectedActivityId.trim().equals(task.getProcessActivityId()))
        && resourceMatches;
  }

  /**
   * Retoma uma única vez callbacks interrompidos ou rejeições corrigíveis do contrato de landing.
   */
  private Optional<AgentTask> recoverInterruptedCallbackOnce(
      String agentKey, String processCode, String activityId, String executionResourceCode) {
    return repository
        .findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            agentKey.trim(), "WORK", "BLOCKED")
        .stream()
        .filter(task -> task.getProcessDefinition() != null)
        .filter(
            task -> matchesExecutionContract(task, processCode, activityId, executionResourceCode))
        .filter(task -> isRetryableCallbackFailure(task.getExecutionError()))
        .findFirst()
        .map(
            task -> {
              Instant now = Instant.now(clock);
              task.setStatus("IN_PROGRESS");
              task.setExecutionError("AUTO_RETRY_ONCE|" + task.getExecutionError());
              task.setUpdatedAt(now);
              AgentTask saved = repository.save(task);
              synchronizeActivityInstance(saved, now);
              return saved;
            });
  }

  /** Distingue indisponibilidade transitória do backend de falha funcional do agente. */
  private boolean isRetryableCallbackFailure(String error) {
    return error != null
        && !error.startsWith("AUTO_RETRY_ONCE|")
        && !error.startsWith("AUTO_RETRY_CALLBACK_ONCE|")
        && (error.startsWith("500 :")
            || error.contains("Internal Server Error")
            || error.contains("HTML integral alterou o destino protegido do checkout"));
  }

  /** Reexpõe a lease ativa ao mesmo executor para permitir retomada após interrupção. */
  @Transactional(readOnly = true)
  public AgentTaskPendingResponse claimedProcessTask(String agentKey, Long taskId) {
    return pendingResponse(claimedBy(agentKey, taskId));
  }

  /** Converte uma tarefa reservada e inclui somente a rota de pesquisa do executor responsável. */
  private AgentTaskPendingResponse pendingResponse(AgentTask task) {
    BusinessProcessDefinition process = task.getProcessDefinition();
    AgentTaskTargetResponse taskTarget =
        taskTargetContextProvider
            .resolve(task.getSourceReference(), process.getProcessCode())
            .orElse(null);
    String processContextJson = processContext(task);
    return new AgentTaskPendingResponse(
        task.getId(),
        task.getAssignedAgent().getAgentKey(),
        process.getProcessCode(),
        process.getVersionNumber(),
        task.getProcessActivityId(),
        task.getProcessActivityName(),
        task.getTitle(),
        task.getDescription(),
        task.getSourceReference(),
        task.getReceivedAt(),
        executionResource(task),
        taskTarget,
        processContextJson,
        researchIntelligenceService == null
            ? null
            : researchIntelligenceService.selectForAgentTask(
                task.getAssignedAgent().getAgentKey(),
                process.getProcessCode(),
                task.getProcessActivityId(),
                task.getTitle(),
                task.getDescription(),
                task.getSourceReference(),
                processContextJson,
                String.valueOf(taskTarget)));
  }

  /** Resolve o recurso exigido pela atividade e entrega instruções oficiais ao executor correto. */
  private AgentTaskExecutionResourceResponse executionResource(AgentTask task) {
    String resourceCode = activityExecutionResourceCode(task);
    if (resourceCode == null) return null;
    BusinessProcessExecutionResource resource = requiredExecutionResource(resourceCode);
    if (!resource.getResponsibleAgentKey().equals(task.getAssignedAgent().getAgentKey())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O recurso especializado não pertence ao agente responsável pela tarefa.");
    }
    return new AgentTaskExecutionResourceResponse(
        resource.getResourceCode(),
        resource.getName(),
        resource.getResourceType(),
        resource.getExecutorReference(),
        resource.getUsageInstructions());
  }

  /** Lê da versão imutável do processo o código de recurso da atividade vinculada. */
  private String activityExecutionResourceCode(AgentTask task) {
    try {
      JsonNode nodes =
          objectMapper.readTree(task.getProcessDefinition().getDiagramJson()).path("nodes");
      for (JsonNode node : nodes) {
        if (task.getProcessActivityId().equals(node.path("id").asText())) {
          return trimToNull(node.path("executionResourceCode").asText(null));
        }
      }
      return null;
    } catch (Exception ex) {
      log.error(
          "Falha ao ler recurso da atividade. taskId={} processDefinitionId={} activityId={}",
          task.getId(),
          task.getProcessDefinition().getId(),
          task.getProcessActivityId(),
          ex);
      throw new IllegalStateException(
          "Não foi possível ler o recurso especializado da atividade.", ex);
    }
  }

  /** Exige recurso ativo para impedir execução silenciosa por container ausente ou aposentado. */
  private BusinessProcessExecutionResource requiredExecutionResource(String resourceCode) {
    if (executionResourceRepository == null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O catálogo de recursos especializados não está disponível.");
    }
    return executionResourceRepository
        .findByResourceCodeAndActiveTrue(resourceCode)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O recurso especializado da atividade não está disponível."));
  }

  /** Consolida resultados predecessores para o próximo agente avaliar evidências reais. */
  private String processContext(AgentTask task) {
    try {
      Map<String, AgentTask> latestByOwnerActivity = new LinkedHashMap<>();
      repository
          .findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
              task.getProcessDefinition().getId(), task.getSourceReference())
          .stream()
          .filter(sibling -> "COMPLETED".equals(sibling.getStatus()))
          .forEach(
              sibling ->
                  latestByOwnerActivity.put(
                      sibling.getAssignedAgent().getAgentKey()
                          + ":"
                          + sibling.getProcessActivityId(),
                      sibling));
      List<Map<String, Object>> completedActivities =
          latestByOwnerActivity.values().stream()
              .sorted(java.util.Comparator.comparing(AgentTask::getId))
              .map(this::agentActivityContext)
              .toList();
      List<Map<String, Object>> completedHumanActivities = completedHumanActivities(task);
      Map<String, Object> context = new java.util.LinkedHashMap<>();
      context.put("completedActivities", completedActivities);
      context.put("completedHumanActivities", completedHumanActivities);
      marketStrategicContextProvider
          .resolve(task.getSourceReference())
          .ifPresent(contract -> context.put("marketStrategicContract", contract));
      if ("communication-director".equals(task.getAssignedAgent().getAgentKey())) {
        communicationMaterializationContextProvider
            .resolve(task.getSourceReference())
            .ifPresent(contract -> context.put("communicationMaterializationContext", contract));
      }
      return objectMapper.writeValueAsString(context);
    } catch (Exception ex) {
      log.error(
          "Falha ao consolidar contexto do processo. taskId={} processDefinitionId={} sourceReference={}",
          task.getId(),
          task.getProcessDefinition().getId(),
          task.getSourceReference(),
          ex);
      throw new IllegalStateException("Não foi possível consolidar o contexto do processo", ex);
    }
  }

  /** Converte uma tarefa concluída em contexto estruturado sem JSON serializado dentro de JSON. */
  private Map<String, Object> agentActivityContext(AgentTask sibling) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("taskId", sibling.getId());
    context.put("agentKey", sibling.getAssignedAgent().getAgentKey());
    context.put("activityId", sibling.getProcessActivityId());
    context.put("activityName", sibling.getProcessActivityName());
    context.put("result", readOptionalJson(sibling.getResultJson(), "resultado", sibling.getId()));
    context.put(
        "evidence", readOptionalJson(sibling.getEvidenceJson(), "evidência", sibling.getId()));
    context.put(
        "deliveredAt",
        sibling.getDeliveredAt() == null ? null : sibling.getDeliveredAt().toString());
    return context;
  }

  /** Lista decisões humanas concluídas para os agentes posteriores avaliarem fatos persistidos. */
  private List<Map<String, Object>> completedHumanActivities(AgentTask task) {
    if (activityInstanceRepository == null) return List.of();
    Map<String, BusinessProcessActivityInstance> latest = new LinkedHashMap<>();
    activityInstanceRepository
        .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
            task.getProcessDefinition().getId(), task.getSourceReference())
        .stream()
        .filter(instance -> "COMPLETED".equals(instance.getStatus()))
        .filter(BusinessProcessActivityInstance::isObjectiveAchieved)
        .filter(instance -> instance.getActivityDefinition() != null)
        .filter(
            instance ->
                instance.getActivityDefinition().getOwnerName() != null
                    && "operador humano"
                        .equalsIgnoreCase(instance.getActivityDefinition().getOwnerName().trim()))
        .forEach(
            instance ->
                latest.merge(
                    instance.getActivityDefinition().getActivityId(),
                    instance,
                    (current, replacement) ->
                        replacement.getOccurrenceNumber() >= current.getOccurrenceNumber()
                            ? replacement
                            : current));
    return latest.values().stream()
        .sorted(java.util.Comparator.comparing(BusinessProcessActivityInstance::getId))
        .map(this::humanActivityContext)
        .toList();
  }

  /** Converte uma decisão humana concluída em evidência própria para o contexto do processo. */
  private Map<String, Object> humanActivityContext(BusinessProcessActivityInstance instance) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("activityInstanceId", instance.getId());
    context.put("activityId", instance.getActivityDefinition().getActivityId());
    context.put("activityName", instance.getActivityDefinition().getName());
    context.put(
        "objectiveEvidence",
        readOptionalJson(
            instance.getObjectiveEvidenceJson(), "evidência humana", instance.getId()));
    context.put(
        "exitedAt", instance.getExitedAt() == null ? null : instance.getExitedAt().toString());
    return context;
  }

  /** Lê JSON opcional preservando objeto nulo e falhando com contexto quando estiver corrompido. */
  private JsonNode readOptionalJson(String raw, String label, Long entityId) {
    if (raw == null || raw.isBlank()) return objectMapper.nullNode();
    try {
      return objectMapper.readTree(raw);
    } catch (Exception ex) {
      log.error("Falha ao ler {} do contexto BPM. entityId={}", label, entityId, ex);
      throw new IllegalStateException("JSON inválido no contexto BPM: " + label + ".", ex);
    }
  }

  /** Conclui trabalho reservado com saída e evidências persistidas. */
  @Transactional
  public void completeClaimedProcessTask(
      String agentKey, Long taskId, CompleteAgentTaskRequest request) {
    AgentTask task = claimedBy(agentKey, taskId);
    Instant now = Instant.now(clock);
    task.setResultJson(request.resultJson());
    task.setEvidenceJson(request.evidenceJson());
    applyExecutionAudit(task, request.executionAudit());
    applyModelUsage(task, request.modelUsages());
    requireTerminalExecutionAudit(task, true);
    task.setExecutionError(null);
    AgentTaskCompletionHook.CompletionDisposition disposition = applyCompletionHooks(task, request);
    if (AgentTaskCompletionHook.CompletionDisposition.DEFERRED.equals(disposition)) {
      task.setUpdatedAt(now);
      AgentTask saved = repository.save(task);
      synchronizeActivityInstance(saved, now);
      return;
    }
    task.setStatus("COMPLETED");
    if (task.getDeliveredAt() == null) task.setDeliveredAt(now);
    task.setUpdatedAt(now);
    AgentTask saved = repository.save(task);
    synchronizeActivityInstance(saved, now);
  }

  /** Executa no máximo um efeito especializado antes da mudança final de status. */
  private AgentTaskCompletionHook.CompletionDisposition applyCompletionHooks(
      AgentTask task, CompleteAgentTaskRequest request) {
    List<AgentTaskCompletionHook> matched =
        completionHooks.stream().filter(hook -> hook.supports(task)).toList();
    if (matched.size() > 1) {
      throw new IllegalStateException("Mais de um handler governa a conclusão da mesma tarefa.");
    }
    return matched.isEmpty()
        ? AgentTaskCompletionHook.CompletionDisposition.COMPLETE
        : matched.getFirst().apply(task, request);
  }

  /** Retorna a identidade persistida do responsável por uma tarefa BPM. */
  @Transactional(readOnly = true)
  public String assignedAgentKey(Long taskId) {
    return repository
        .findById(taskId)
        .map(task -> task.getAssignedAgent().getAgentKey())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));
  }

  /** Conclui uma materialização cuja aprovação técnica assíncrona foi persistida. */
  @Transactional
  public void completeDeferredProcessTask(
      String agentKey, Long taskId, String technicalEvidenceJson) {
    AgentTask task = claimedBy(agentKey, taskId);
    Instant now = Instant.now(clock);
    requireTerminalExecutionAudit(task, true);
    task.setEvidenceJson(mergeEvidence(task.getEvidenceJson(), technicalEvidenceJson));
    task.setExecutionError(null);
    task.setStatus("COMPLETED");
    task.setDeliveredAt(now);
    task.setUpdatedAt(now);
    AgentTask saved = repository.save(task);
    synchronizeActivityInstance(saved, now);
  }

  /** Bloqueia uma materialização quando o gate técnico assíncrono rejeita o artefato. */
  @Transactional
  public void failDeferredProcessTask(
      String agentKey, Long taskId, String error, String technicalEvidenceJson) {
    AgentTask task = claimedBy(agentKey, taskId);
    Instant now = Instant.now(clock);
    task.setEvidenceJson(mergeEvidence(task.getEvidenceJson(), technicalEvidenceJson));
    task.setExecutionError(error);
    ensurePreModelFailureAudit(task, null, null);
    applyBlockerGuidance(task, null, error, null);
    requireTerminalExecutionAudit(task, false);
    task.setStatus("BLOCKED");
    task.setUpdatedAt(now);
    AgentTask saved = repository.save(task);
    synchronizeActivityInstance(saved, now);
  }

  /** Separa a evidência de materialização do parecer técnico posterior. */
  private String mergeEvidence(String materializationEvidenceJson, String technicalEvidenceJson) {
    try {
      Map<String, Object> evidence = new java.util.LinkedHashMap<>();
      evidence.put(
          "materialization",
          materializationEvidenceJson == null
              ? null
              : objectMapper.readTree(materializationEvidenceJson));
      evidence.put(
          "technicalGate",
          technicalEvidenceJson == null ? null : objectMapper.readTree(technicalEvidenceJson));
      return objectMapper.writeValueAsString(evidence);
    } catch (Exception ex) {
      log.error(
          "Falha ao consolidar evidências de gate assíncrono. taskEvidencePresent={} technicalEvidencePresent={}",
          materializationEvidenceJson != null,
          technicalEvidenceJson != null,
          ex);
      throw new IllegalArgumentException(
          "Evidência do gate assíncrono não contém JSON válido.", ex);
    }
  }

  /** Bloqueia trabalho reservado preservando a causa técnica completa. */
  @Transactional
  public void failClaimedProcessTask(String agentKey, Long taskId, FailAgentTaskRequest request) {
    AgentTask task = claimedBy(agentKey, taskId);
    task.setExecutionError(request.error());
    task.setResultJson(request.resultJson());
    task.setEvidenceJson(request.evidenceJson());
    applyExecutionAudit(task, request.executionAudit());
    applyModelUsage(task, request.modelUsages());
    ensurePreModelFailureAudit(task, request.resultJson(), request.modelUsages());
    applyBlockerGuidance(task, request.blockerGuidance(), request.error(), request.resultJson());
    requireTerminalExecutionAudit(task, false);
    task.setStatus("BLOCKED");
    Instant now = Instant.now(clock);
    task.setUpdatedAt(now);
    AgentTask saved = repository.save(task);
    synchronizeActivityInstance(saved, now);
  }

  /** Acumula tokens reais e custo estimado sem permitir que o executor escolha as tarifas. */
  private void applyModelUsage(AgentTask task, List<AgentTaskModelUsageRequest> usages) {
    if (usages == null) return;
    Instant now = Instant.now(clock);
    if (usages.isEmpty()) {
      if ("MODEL".equals(task.getExecutionMode())) {
        return;
      }
      if (task.getInputTokens() == null) {
        task.setInputTokens(0L);
        task.setCachedInputTokens(0L);
        task.setOutputTokens(0L);
        task.setEstimatedCostUsd(BigDecimal.ZERO.setScale(8));
        task.setCostEstimationStatus("NOT_APPLICABLE");
        task.setModelUsageUpdatedAt(now);
      }
      return;
    }
    long input = task.getInputTokens() == null ? 0 : task.getInputTokens();
    long cached = task.getCachedInputTokens() == null ? 0 : task.getCachedInputTokens();
    long output = task.getOutputTokens() == null ? 0 : task.getOutputTokens();
    BigDecimal knownCost =
        task.getEstimatedCostUsd() == null
            ? BigDecimal.ZERO.setScale(8)
            : task.getEstimatedCostUsd();
    boolean hasKnownCost = task.getEstimatedCostUsd() != null;
    boolean missingPrice =
        "PRICING_UNAVAILABLE".equals(task.getCostEstimationStatus())
            || "PARTIALLY_ESTIMATED".equals(task.getCostEstimationStatus());
    for (AgentTaskModelUsageRequest usage : usages) {
      validateModelUsage(usage);
      input = Math.addExact(input, usage.inputTokens());
      cached = Math.addExact(cached, usage.cachedInputTokens());
      output = Math.addExact(output, usage.outputTokens());
      Optional<BigDecimal> estimated =
          pricingService == null
              ? Optional.empty()
              : pricingService.estimateTaskCost(
                  usage.modelCode(),
                  usage.serviceTier(),
                  usage.inputTokens(),
                  usage.cachedInputTokens(),
                  usage.outputTokens());
      if (estimated.isPresent()) {
        knownCost = knownCost.add(estimated.get());
        hasKnownCost = true;
      } else {
        missingPrice = true;
      }
    }
    task.setInputTokens(input);
    task.setCachedInputTokens(cached);
    task.setOutputTokens(output);
    task.setEstimatedCostUsd(hasKnownCost ? knownCost.setScale(8) : null);
    task.setCostEstimationStatus(
        missingPrice
            ? (hasKnownCost ? "PARTIALLY_ESTIMATED" : "PRICING_UNAVAILABLE")
            : "ESTIMATED");
    task.setModelUsageUpdatedAt(now);
  }

  /** Preserva modo, modelo, esforço, partes do prompt integral e URLs acessadas na tentativa. */
  private void applyExecutionAudit(AgentTask task, AgentTaskExecutionAuditRequest audit) {
    if (audit == null) return;
    String mode = normalizedUpper(audit.executionMode());
    if (!EXECUTION_MODES.contains(mode)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modo de execução inválido.");
    }
    task.setExecutionMode(mode);
    task.setExecutionModelCode(trimToNull(audit.modelCode()));
    task.setExecutionReasoningEffort(trimToNull(audit.reasoningEffort()));
    task.setExecutionPrompt(audit.promptSent());
    task.setExecutionAgentPrompt(audit.agentPromptPart());
    task.setExecutionActivityPrompt(audit.activityPromptPart());
    if ("DETERMINISTIC".equals(mode) && trimToNull(audit.promptSent()) != null) {
      task.setExecutionAgentPrompt(null);
      task.setExecutionActivityPrompt(audit.promptSent());
    }
    if ("NOT_STARTED".equals(mode)) {
      task.setExecutionModelCode(null);
      task.setExecutionReasoningEffort("NOT_APPLICABLE");
      task.setExecutionPrompt(null);
      task.setExecutionAgentPrompt(null);
      task.setExecutionActivityPrompt(null);
    }
    replaceAccessedUrls(task, audit.accessedUrls());
  }

  /** Exige uma auditoria coerente sem transformar ausência em configuração inventada. */
  private void requireExecutionAuditShape(AgentTask task, boolean terminalCompletion) {
    String mode = trimToNull(task.getExecutionMode());
    if (mode == null || !EXECUTION_MODES.contains(mode)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A tarefa exige modo de execução auditável.");
    }
    if ("MODEL".equals(mode)
        && (trimToNull(task.getExecutionModelCode()) == null
            || trimToNull(task.getExecutionReasoningEffort()) == null
            || trimToNull(task.getExecutionPrompt()) == null
            || trimToNull(task.getExecutionAgentPrompt()) == null
            || trimToNull(task.getExecutionActivityPrompt()) == null)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Execução de modelo exige modelo, tipo de raciocínio, parte do agente, parte da atividade e prompt integral.");
    }
    if ("MODEL".equals(mode)) validatePromptComposition(task);
    if ("DETERMINISTIC".equals(mode)
        && (!"NOT_APPLICABLE".equals(task.getExecutionReasoningEffort())
            || trimToNull(task.getExecutionModelCode()) == null
            || trimToNull(task.getExecutionPrompt()) == null)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Execução determinística exige identificador, entrada integral e raciocínio não aplicável.");
    }
    if (terminalCompletion && "NOT_STARTED".equals(mode)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Tarefa concluída não pode declarar modelo não iniciado.");
    }
  }

  /** Aplica a validação terminal comum antes de concluir ou bloquear uma tarefa. */
  private void requireTerminalExecutionAudit(AgentTask task, boolean completed) {
    requireExecutionAuditShape(task, completed);
  }

  /** Registra a entrada integral de um comando sem modelo quando nenhuma auditoria já existe. */
  private void ensureDeterministicCommandAudit(
      AgentTask task, String executionIdentifier, Map<String, Object> fullInput) {
    if (trimToNull(task.getExecutionMode()) != null) return;
    try {
      task.setExecutionMode("DETERMINISTIC");
      task.setExecutionModelCode(executionIdentifier);
      task.setExecutionReasoningEffort("NOT_APPLICABLE");
      String serializedInput = objectMapper.writeValueAsString(fullInput);
      task.setExecutionPrompt(serializedInput);
      task.setExecutionAgentPrompt(null);
      task.setExecutionActivityPrompt(serializedInput);
      task.setInputTokens(0L);
      task.setCachedInputTokens(0L);
      task.setOutputTokens(0L);
      task.setEstimatedCostUsd(BigDecimal.ZERO.setScale(8));
      task.setCostEstimationStatus("NOT_APPLICABLE");
      task.setModelUsageUpdatedAt(Instant.now(clock));
    } catch (Exception ex) {
      log.error(
          "Falha ao registrar entrada determinística da tarefa. taskId={} executionIdentifier={}",
          task.getId(),
          executionIdentifier,
          ex);
      throw new IllegalStateException("Não foi possível auditar o comando determinístico.", ex);
    }
  }

  /** Registra explicitamente uma falha anterior ao modelo quando não houve saída nem consumo. */
  private void ensurePreModelFailureAudit(
      AgentTask task, String resultJson, List<AgentTaskModelUsageRequest> modelUsages) {
    if (trimToNull(task.getExecutionMode()) != null) return;
    if (trimToNull(resultJson) != null || (modelUsages != null && !modelUsages.isEmpty())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Falha após uso do modelo exige prompt e tipo de raciocínio auditados.");
    }
    task.setExecutionMode("NOT_STARTED");
    task.setExecutionModelCode(null);
    task.setExecutionReasoningEffort("NOT_APPLICABLE");
    task.setExecutionPrompt(null);
    task.setExecutionAgentPrompt(null);
    task.setExecutionActivityPrompt(null);
  }

  /** Confirma que as duas partes declaradas pertencem, na ordem correta, ao prompt enviado. */
  private void validatePromptComposition(AgentTask task) {
    String fullPrompt = task.getExecutionPrompt();
    String agentPrompt = task.getExecutionAgentPrompt();
    String activityPrompt = task.getExecutionActivityPrompt();
    if (matchesPhasedPromptComposition(fullPrompt, agentPrompt, activityPrompt)) return;
    int agentStart = fullPrompt.indexOf(agentPrompt);
    int activityStart =
        agentStart < 0 ? -1 : fullPrompt.indexOf(activityPrompt, agentStart + agentPrompt.length());
    if (agentStart < 0 || activityStart < 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "As partes do agente e da atividade devem compor, nesta ordem, o prompt integral.");
    }
  }

  /** Valida cada interação de um prompt multifase sem reordenar o que foi enviado ao modelo. */
  private boolean matchesPhasedPromptComposition(
      String fullPrompt, String agentPrompt, String activityPrompt) {
    Map<String, String> fullPhases = promptPhases(fullPrompt);
    Map<String, String> agentPhases = promptPhases(agentPrompt);
    Map<String, String> activityPhases = promptPhases(activityPrompt);
    if (fullPhases.isEmpty()
        || !List.copyOf(fullPhases.keySet()).equals(List.copyOf(agentPhases.keySet()))
        || !List.copyOf(fullPhases.keySet()).equals(List.copyOf(activityPhases.keySet()))) {
      return false;
    }
    return fullPhases.entrySet().stream()
        .allMatch(
            phase ->
                phase
                    .getValue()
                    .equals(
                        agentPhases.get(phase.getKey())
                            + "\n\n"
                            + activityPhases.get(phase.getKey())));
  }

  /** Separa os blocos nomeados do dossiê de prompts preservando sua ordem declarada. */
  private Map<String, String> promptPhases(String prompt) {
    Map<String, String> phases = new LinkedHashMap<>();
    Matcher matcher = PROMPT_PHASE_HEADER.matcher(prompt);
    String currentName = null;
    int contentStart = -1;
    while (matcher.find()) {
      if (currentName != null) {
        if (phases.putIfAbsent(currentName, prompt.substring(contentStart, matcher.start()).trim())
            != null) {
          return Map.of();
        }
      }
      currentName = matcher.group(1).trim();
      contentStart = matcher.end();
    }
    if (currentName != null
        && phases.putIfAbsent(currentName, prompt.substring(contentStart).trim()) != null) {
      return Map.of();
    }
    return phases;
  }

  /** Persiste orientação explícita ou deriva um fallback acionável sem depender de logs. */
  private void applyBlockerGuidance(
      AgentTask task, AgentTaskBlockerGuidanceRequest guidance, String error, String resultJson) {
    String category =
        guidance == null
            ? (trimToNull(resultJson) == null ? "TECHNICAL_FAILURE" : "FUNCTIONAL_ADJUSTMENT")
            : normalizedUpper(guidance.category());
    if (!BLOCKER_CATEGORIES.contains(category)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria de bloqueio inválida.");
    }
    String action =
        guidance == null
            ? derivedBlockerAction(resultJson, error)
            : trimToNull(guidance.recommendedAction());
    if (action == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Bloqueio exige orientação de correção.");
    }
    List<AgentTaskHelpLinkRequest> links =
        guidance == null
            ? defaultHelpLinks(task)
            : guidance.helpLinks() == null ? List.of() : guidance.helpLinks();
    if (links.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Bloqueio exige ao menos um link de ajuda.");
    }
    task.setBlockerCategory(category);
    task.setBlockerAction(action);
    replaceHelpLinks(task, links);
  }

  /** Extrai a mudança pedida pelo parecer e mantém fallback técnico simples. */
  private String derivedBlockerAction(String resultJson, String error) {
    if (trimToNull(resultJson) != null) {
      try {
        JsonNode result = objectMapper.readTree(resultJson);
        for (String field : List.of("recommendedAction", "nextAction", "remediation")) {
          String direct = textValue(result.path(field));
          if (direct != null) return direct;
        }
        JsonNode changes = result.path("requiredChanges");
        if (changes.isArray() && !changes.isEmpty()) {
          List<String> actions = new ArrayList<>();
          for (JsonNode change : changes) {
            String action =
                change.isTextual()
                    ? trimToNull(change.asText())
                    : firstText(change, "action", "description", "change", "requiredChange");
            if (action != null) actions.add(action);
            if (actions.size() == 3) break;
          }
          if (!actions.isEmpty())
            return "Corrija antes de reiniciar: " + String.join("; ", actions);
        }
      } catch (Exception ex) {
        log.warn("Resultado bloqueado não contém JSON legível. error={}", error, ex);
      }
    }
    String cause = trimToNull(error);
    return cause == null
        ? "Revise a causa registrada e reinicie a tarefa depois da correção."
        : "Corrija a causa registrada e reinicie a tarefa: " + cause;
  }

  /** Monta links seguros mínimos e acrescenta o artefato público quando ele foi congelado. */
  private List<AgentTaskHelpLinkRequest> defaultHelpLinks(AgentTask task) {
    List<AgentTaskHelpLinkRequest> links = new ArrayList<>();
    links.add(new AgentTaskHelpLinkRequest("Abrir tarefas dos agentes", "/agent-tasks"));
    try {
      JsonNode evidence =
          trimToNull(task.getEvidenceJson()) == null
              ? objectMapper.createObjectNode()
              : objectMapper.readTree(task.getEvidenceJson());
      String publicUrl = textValue(evidence.path("taskTarget").path("publicUrl"));
      if (publicUrl != null) {
        links.add(new AgentTaskHelpLinkRequest("Abrir experiência revisada", publicUrl));
      }
    } catch (Exception ex) {
      log.debug("Evidência sem alvo navegável na tarefa {}.", task.getId(), ex);
    }
    return List.copyOf(links);
  }

  /** Substitui somente as URLs de acesso, preservando os links de orientação. */
  private void replaceAccessedUrls(AgentTask task, List<AgentTaskAccessedUrlRequest> accessedUrls) {
    if (accessedUrls == null) return;
    task.getAuditLinks().removeIf(link -> ACCESSED_URL_LINK.equals(link.getLinkType()));
    Map<String, AgentTaskAccessedUrlRequest> unique = new java.util.LinkedHashMap<>();
    for (AgentTaskAccessedUrlRequest access : accessedUrls) {
      if (access == null) continue;
      String url = validatedUrl(access.url(), false);
      String method = normalizedUpper(access.accessMethod());
      if (!ACCESS_METHODS.contains(method)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Método de acesso da URL inválido.");
      }
      unique.putIfAbsent(
          url,
          new AgentTaskAccessedUrlRequest(
              url,
              requiredText(access.label(), "Rótulo da URL acessada"),
              method,
              access.accessedAt()));
    }
    int order = 0;
    for (AgentTaskAccessedUrlRequest access : unique.values()) {
      addAuditLink(
          task,
          ACCESSED_URL_LINK,
          access.label(),
          access.url(),
          access.accessMethod(),
          access.accessedAt(),
          order++);
    }
  }

  /** Substitui os links de ajuda depois de validar protocolo e ausência de credenciais. */
  private void replaceHelpLinks(AgentTask task, List<AgentTaskHelpLinkRequest> helpLinks) {
    task.getAuditLinks().removeIf(link -> BLOCKER_HELP_LINK.equals(link.getLinkType()));
    Map<String, AgentTaskHelpLinkRequest> unique = new java.util.LinkedHashMap<>();
    for (AgentTaskHelpLinkRequest help : helpLinks) {
      if (help == null) continue;
      String url = validatedUrl(help.url(), true);
      unique.putIfAbsent(
          url,
          new AgentTaskHelpLinkRequest(requiredText(help.label(), "Rótulo do link de ajuda"), url));
    }
    int order = 0;
    for (AgentTaskHelpLinkRequest help : unique.values()) {
      addAuditLink(task, BLOCKER_HELP_LINK, help.label(), help.url(), null, null, order++);
    }
  }

  /** Adiciona um link já validado à coleção governada pela tarefa. */
  private void addAuditLink(
      AgentTask task,
      String linkType,
      String label,
      String url,
      String accessMethod,
      Instant accessedAt,
      int displayOrder) {
    AgentTaskAuditLink link = new AgentTaskAuditLink();
    link.setTask(task);
    link.setLinkType(linkType);
    link.setLabel(label);
    link.setUrl(url);
    link.setAccessMethod(accessMethod);
    link.setAccessedAt(accessedAt);
    link.setDisplayOrder(displayOrder);
    link.setCreatedAt(Instant.now(clock));
    task.getAuditLinks().add(link);
  }

  /** Aceita somente links internos ou HTTP seguros e rejeita parâmetros de credencial. */
  private String validatedUrl(String value, boolean allowRelative) {
    String normalized = trimToNull(value);
    if (normalized == null || normalized.length() > 2048) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL de auditoria inválida.");
    }
    try {
      URI uri = URI.create(normalized);
      if (allowRelative
          && uri.getScheme() == null
          && uri.getRawAuthority() == null
          && uri.getRawPath() != null
          && uri.getRawPath().startsWith("/")) {
        requireUrlWithoutCredentials(uri);
        return uri.toString();
      }
      if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
          || trimToNull(uri.getHost()) == null
          || uri.getUserInfo() != null) {
        throw new IllegalArgumentException("protocolo, host ou usuário inválido");
      }
      requireUrlWithoutCredentials(uri);
      return uri.toString();
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL de auditoria insegura.", ex);
    }
  }

  /** Rejeita credenciais em query ou fragmento inclusive quando o link é interno. */
  private void requireUrlWithoutCredentials(URI uri) {
    for (String section : new String[] {uri.getRawQuery(), uri.getRawFragment()}) {
      if (section == null) continue;
      for (String parameter : section.split("[?&;]")) {
        String rawName = parameter.split("=", 2)[0];
        String name =
            URLDecoder.decode(rawName, StandardCharsets.UTF_8)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        if (SENSITIVE_QUERY_PARAMETERS.contains(name)) {
          throw new IllegalArgumentException("parâmetro sensível");
        }
      }
    }
  }

  /** Normaliza um valor obrigatório sem alterar seu conteúdo interno. */
  private String requiredText(String value, String label) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " é obrigatório.");
    }
    return normalized;
  }

  /** Normaliza vocabulários contratuais em maiúsculas. */
  private String normalizedUpper(String value) {
    String normalized = trimToNull(value);
    return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
  }

  /** Lê o primeiro texto preenchido de uma saída estruturada. */
  private String firstText(JsonNode node, String... fields) {
    for (String field : fields) {
      String value = textValue(node.path(field));
      if (value != null) return value;
    }
    return null;
  }

  /** Converte somente nós textuais preenchidos em orientação. */
  private String textValue(JsonNode node) {
    return node != null && node.isTextual() ? trimToNull(node.asText()) : null;
  }

  /** Revalida contadores para chamadas internas que não passam pela validação HTTP. */
  private void validateModelUsage(AgentTaskModelUsageRequest usage) {
    if (usage == null
        || usage.modelCode() == null
        || usage.modelCode().isBlank()
        || !supportedServiceTier(usage.serviceTier())
        || usage.inputTokens() == null
        || usage.cachedInputTokens() == null
        || usage.outputTokens() == null
        || usage.inputTokens() < 0
        || usage.cachedInputTokens() < 0
        || usage.outputTokens() < 0
        || usage.cachedInputTokens() > usage.inputTokens()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consumo de modelo inválido.");
    }
  }

  /** Aceita somente os tiers para os quais o catálogo possui política de preço definida. */
  private boolean supportedServiceTier(String serviceTier) {
    return serviceTier != null
        && ("STANDARD".equalsIgnoreCase(serviceTier)
            || "FLEX".equalsIgnoreCase(serviceTier)
            || "BATCH".equalsIgnoreCase(serviceTier));
  }

  /** Confirma identidade e lease antes de aceitar callback operacional. */
  private AgentTask claimedBy(String agentKey, Long taskId) {
    AgentTask task = task(taskId);
    if (!task.getAssignedAgent().getAgentKey().equals(agentKey.trim())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tarefa pertence a outro agente.");
    }
    if (!"IN_PROGRESS".equals(task.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Tarefa não está reservada.");
    }
    return task;
  }

  /** Verifica no grafo se todas as atividades imediatamente anteriores foram concluídas. */
  private boolean predecessorsCompleted(AgentTask candidate) {
    try {
      JsonNode diagram = objectMapper.readTree(candidate.getProcessDefinition().getDiagramJson());
      Map<String, List<String>> incoming = new HashMap<>();
      Map<String, List<String>> outgoing = new HashMap<>();
      Set<String> graphNodes = new HashSet<>();
      JsonNode connections =
          diagram.path("flows").isArray() ? diagram.path("flows") : diagram.path("edges");
      for (JsonNode edge : connections) {
        String target =
            edge.hasNonNull("to") ? edge.path("to").asText() : edge.path("target").asText();
        String source =
            edge.hasNonNull("from") ? edge.path("from").asText() : edge.path("source").asText();
        incoming.computeIfAbsent(target, ignored -> new ArrayList<>()).add(source);
        outgoing.computeIfAbsent(source, ignored -> new ArrayList<>()).add(target);
        graphNodes.add(source);
        graphNodes.add(target);
      }
      Map<String, Integer> distanceFromStart =
          shortestDistancesFromStart(incoming, outgoing, graphNodes);
      List<AgentTask> siblings =
          repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
              candidate.getProcessDefinition().getId(), candidate.getSourceReference());
      Map<String, Map<String, AgentTask>> latestTasksByActivityAndOwner = new HashMap<>();
      siblings.forEach(
          sibling ->
              latestTasksByActivityAndOwner
                  .computeIfAbsent(sibling.getProcessActivityId(), ignored -> new HashMap<>())
                  .merge(
                      sibling.getAssignedAgent().getAgentKey(),
                      sibling,
                      (current, replacement) ->
                          replacement.getId() > current.getId() ? replacement : current));
      Map<String, BusinessProcessActivityInstance> latestInstancesByActivity = new HashMap<>();
      if (activityInstanceRepository != null) {
        activityInstanceRepository
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                candidate.getProcessDefinition().getId(), candidate.getSourceReference())
            .stream()
            .filter(instance -> instance.getActivityDefinition() != null)
            .forEach(
                instance ->
                    latestInstancesByActivity.merge(
                        instance.getActivityDefinition().getActivityId(),
                        instance,
                        (current, replacement) ->
                            replacement.getOccurrenceNumber() >= current.getOccurrenceNumber()
                                ? replacement
                                : current));
      }
      Set<String> executionNodes = new HashSet<>();
      siblings.stream().map(AgentTask::getProcessActivityId).forEach(executionNodes::add);
      executionNodes.addAll(latestInstancesByActivity.keySet());
      Set<String> predecessors = new HashSet<>();
      Set<String> visited = new HashSet<>();
      ArrayDeque<String> queue = new ArrayDeque<>();
      queue.add(candidate.getProcessActivityId());
      while (!queue.isEmpty()) {
        String current = queue.removeFirst();
        for (String source : incoming.getOrDefault(current, List.of())) {
          if (distanceFromStart.getOrDefault(source, Integer.MAX_VALUE)
              >= distanceFromStart.getOrDefault(current, Integer.MAX_VALUE)) continue;
          if (!visited.add(source)) continue;
          if (executionNodes.contains(source)) predecessors.add(source);
          else queue.addLast(source);
        }
      }
      return predecessors.stream()
          .allMatch(
              predecessor ->
                  completedPredecessor(
                      latestInstancesByActivity.get(predecessor),
                      latestTasksByActivityAndOwner.get(predecessor)));
    } catch (Exception ex) {
      log.error(
          "Falha ao avaliar sequência BPM. taskId={} processDefinitionId={} activityId={}",
          candidate.getId(),
          candidate.getProcessDefinition().getId(),
          candidate.getProcessActivityId(),
          ex);
      throw new IllegalStateException(
          "Não foi possível avaliar a sequência BPM da tarefa " + candidate.getId(), ex);
    }
  }

  /** Usa a instância como autoridade e mantém tarefas antigas apenas como compatibilidade. */
  private boolean completedPredecessor(
      BusinessProcessActivityInstance instance, Map<String, AgentTask> historicalTasks) {
    if (instance != null) {
      return "COMPLETED".equals(instance.getStatus()) && instance.isObjectiveAchieved();
    }
    return historicalTasks != null
        && !historicalTasks.isEmpty()
        && historicalTasks.values().stream().allMatch(task -> "COMPLETED".equals(task.getStatus()));
  }

  /**
   * Calcula a progressão inicial do processo sem transformar laços de retrabalho em dependências.
   */
  private Map<String, Integer> shortestDistancesFromStart(
      Map<String, List<String>> incoming,
      Map<String, List<String>> outgoing,
      Set<String> graphNodes) {
    Map<String, Integer> distances = new HashMap<>();
    ArrayDeque<String> queue = new ArrayDeque<>();
    graphNodes.stream()
        .filter(node -> incoming.getOrDefault(node, List.of()).isEmpty())
        .forEach(
            node -> {
              distances.put(node, 0);
              queue.addLast(node);
            });
    while (!queue.isEmpty()) {
      String source = queue.removeFirst();
      int nextDistance = distances.get(source) + 1;
      for (String target : outgoing.getOrDefault(source, List.of())) {
        if (nextDistance >= distances.getOrDefault(target, Integer.MAX_VALUE)) continue;
        distances.put(target, nextDistance);
        queue.addLast(target);
      }
    }
    return distances;
  }
}
