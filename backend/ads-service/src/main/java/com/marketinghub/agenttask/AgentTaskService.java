package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocessresource.BusinessProcessExecutionResource;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  private final AgentTaskRepository repository;
  private final AgentRepository agentRepository;
  private final BusinessProcessDefinitionRepository processRepository;
  private final BusinessProcessExecutionResourceRepository executionResourceRepository;
  private final ObjectMapper objectMapper;
  private final OpenAiPricingService pricingService;
  private final Clock clock;

  /** Configura persistência, catálogo e relógio operacional. */
  @Autowired
  public AgentTaskService(
      AgentTaskRepository repository,
      AgentRepository agentRepository,
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessExecutionResourceRepository executionResourceRepository,
      ObjectMapper objectMapper,
      OpenAiPricingService pricingService) {
    this(
        repository,
        agentRepository,
        processRepository,
        executionResourceRepository,
        objectMapper,
        pricingService,
        Clock.systemUTC());
  }

  /** Permite testes determinísticos do histórico temporal. */
  AgentTaskService(
      AgentTaskRepository repository,
      AgentRepository agentRepository,
      BusinessProcessDefinitionRepository processRepository,
      ObjectMapper objectMapper,
      Clock clock) {
    this(repository, agentRepository, processRepository, null, objectMapper, null, clock);
  }

  /** Permite testes determinísticos do custo calculado pelo catálogo. */
  AgentTaskService(
      AgentTaskRepository repository,
      AgentRepository agentRepository,
      BusinessProcessDefinitionRepository processRepository,
      ObjectMapper objectMapper,
      OpenAiPricingService pricingService,
      Clock clock) {
    this(repository, agentRepository, processRepository, null, objectMapper, pricingService, clock);
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
    this.repository = repository;
    this.agentRepository = agentRepository;
    this.processRepository = processRepository;
    this.executionResourceRepository = executionResourceRepository;
    this.objectMapper = objectMapper;
    this.pricingService = pricingService;
    this.clock = clock;
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

  /** Abre uma delegação entre agentes preservando remetente e destinatário. */
  @Transactional
  public AgentTaskResponse createByAgent(CreateAgentTaskByAgentRequest request) {
    Agent requester = agent(request.requestedByAgentKey());
    Agent assignee = agent(request.assignedAgentKey());
    return save(
        assignee,
        requester,
        "AGENT",
        requester.getNickname(),
        request.title(),
        request.description(),
        request.priority(),
        request.sourceReference(),
        null);
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
    return repository
        .findTopByAssignedAgentAgentKeyAndSourceReferenceOrderByUpdatedAtDescIdDesc(
            request.assignedAgentKey().trim(), sourceReference)
        .map(this::response)
        .orElseGet(() -> createByAgent(request));
  }

  /** Sincroniza a tarefa operacional com o resultado efetivo do executor responsável. */
  @Transactional
  public void finishOperationalDelegation(
      String assignedAgentKey, String sourceReference, boolean successful) {
    repository
        .findTopByAssignedAgentAgentKeyAndSourceReferenceOrderByUpdatedAtDescIdDesc(
            assignedAgentKey, sourceReference)
        .ifPresent(
            task -> {
              if (List.of("COMPLETED", "CANCELLED").contains(task.getStatus())) return;
              Instant now = Instant.now(clock);
              task.setStatus(successful ? "COMPLETED" : "BLOCKED");
              if (successful && task.getDeliveredAt() == null) {
                task.setDeliveredAt(now);
              }
              task.setUpdatedAt(now);
              repository.save(task);
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
    return response(repository.save(task));
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
    task.setGateStatus(request.decision());
    task.setGateDecisionReason(request.reason().trim());
    task.setGateDecidedAt(now);
    task.setStatus("APPROVED".equals(request.decision()) ? "COMPLETED" : "BLOCKED");
    if ("APPROVED".equals(request.decision()) && task.getDeliveredAt() == null) {
      task.setDeliveredAt(now);
    }
    task.setUpdatedAt(now);
    return response(repository.save(task));
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

  /** Calcula uma única instância sem persistir estado derivado concorrente. */
  private ProcessInstanceResponse processInstance(
      String sourceReference, List<AgentTask> tasks, List<AgentTask> legacy) {
    BusinessProcessDefinition process = tasks.get(0).getProcessDefinition();
    List<ProcessInstanceTaskResponse> items =
        tasks.stream().map(task -> processInstanceTask(task, false)).toList();
    List<ProcessInstanceTaskResponse> superseded =
        legacy.stream().map(task -> processInstanceTask(task, true)).toList();
    return new ProcessInstanceResponse(
        process.getId(),
        process.getProcessCode(),
        process.getVersionNumber(),
        sourceReference,
        items,
        superseded);
  }

  /** Traduz o status persistido e a elegibilidade do grafo em situação legível. */
  private ProcessInstanceTaskResponse processInstanceTask(
      AgentTask task, boolean supersededLegacy) {
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
        task.getProcessActivityId(),
        task.getProcessActivityName() == null ? task.getTitle() : task.getProcessActivityName(),
        task.getAssignedAgent().getAgentKey(),
        task.getAssignedAgent().getNickname(),
        task.getStatus(),
        state,
        reason,
        failureAudit(task),
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
    Instant now = Instant.now(clock);
    task.setStatus(next);
    if ("COMPLETED".equals(next) && task.getDeliveredAt() == null) {
      task.setDeliveredAt(now);
    }
    task.setUpdatedAt(now);
    return response(repository.save(task));
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
    task.setProcessDefinition(binding.definition());
    task.setProcessActivityId(binding.activityId());
    task.setProcessActivityName(binding.activityName());
    task.setExceptional(false);
    task.setExceptionReason(null);
    task.setUpdatedAt(Instant.now(clock));
    return response(repository.save(task));
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
    task.setExceptional(binding != null && binding.exceptional());
    task.setExceptionReason(binding == null ? null : binding.exceptionReason());
    task.setReceivedAt(null);
    task.setCreatedAt(now);
    task.setUpdatedAt(now);
    return response(repository.save(task));
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
      return new ProcessBinding(null, null, "Atividade excepcional", true, reason);
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
          if (!owner.isEmpty() && !activityOwnerMatchesAgent(owner, assignee)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A atividade selecionada pertence a outro responsável.");
          }
          validateExecutionResourceAssignee(node, assignee);
          return new ProcessBinding(
              definition, node.path("id").asText(), node.path("label").asText(), false, null);
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
      repository.save(task);
      return Optional.of(pendingResponse(task));
    }
    return Optional.empty();
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
              task.setStatus("IN_PROGRESS");
              task.setExecutionError("AUTO_RETRY_ONCE|" + task.getExecutionError());
              task.setUpdatedAt(Instant.now(clock));
              return repository.save(task);
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

  /** Converte uma tarefa reservada no contrato estável entregue ao executor. */
  private AgentTaskPendingResponse pendingResponse(AgentTask task) {
    BusinessProcessDefinition process = task.getProcessDefinition();
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
        processContext(task));
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
    List<Map<String, Object>> completedActivities =
        repository
            .findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
                task.getProcessDefinition().getId(), task.getSourceReference())
            .stream()
            .filter(sibling -> "COMPLETED".equals(sibling.getStatus()))
            .map(
                sibling -> {
                  Map<String, Object> context = new java.util.LinkedHashMap<>();
                  context.put("taskId", sibling.getId());
                  context.put("activityId", sibling.getProcessActivityId());
                  context.put("activityName", sibling.getProcessActivityName());
                  context.put("resultJson", sibling.getResultJson());
                  context.put("evidenceJson", sibling.getEvidenceJson());
                  context.put("deliveredAt", sibling.getDeliveredAt());
                  return context;
                })
            .toList();
    try {
      return objectMapper.writeValueAsString(Map.of("completedActivities", completedActivities));
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

  /** Conclui trabalho reservado com saída e evidências persistidas. */
  @Transactional
  public void completeClaimedProcessTask(
      String agentKey, Long taskId, CompleteAgentTaskRequest request) {
    AgentTask task = claimedBy(agentKey, taskId);
    Instant now = Instant.now(clock);
    task.setResultJson(request.resultJson());
    task.setEvidenceJson(request.evidenceJson());
    applyModelUsage(task, request.modelUsages());
    task.setExecutionError(null);
    task.setStatus("COMPLETED");
    if (task.getDeliveredAt() == null) task.setDeliveredAt(now);
    task.setUpdatedAt(now);
    repository.save(task);
  }

  /** Bloqueia trabalho reservado preservando a causa técnica completa. */
  @Transactional
  public void failClaimedProcessTask(String agentKey, Long taskId, FailAgentTaskRequest request) {
    AgentTask task = claimedBy(agentKey, taskId);
    task.setExecutionError(request.error());
    task.setResultJson(request.resultJson());
    task.setEvidenceJson(request.evidenceJson());
    applyModelUsage(task, request.modelUsages());
    task.setStatus("BLOCKED");
    task.setUpdatedAt(Instant.now(clock));
    repository.save(task);
  }

  /** Acumula tokens reais e custo estimado sem permitir que o executor escolha as tarifas. */
  private void applyModelUsage(AgentTask task, List<AgentTaskModelUsageRequest> usages) {
    if (usages == null) return;
    Instant now = Instant.now(clock);
    if (usages.isEmpty()) {
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
      Set<String> completed = new HashSet<>();
      siblings.stream()
          .filter(task -> "COMPLETED".equals(task.getStatus()))
          .map(AgentTask::getProcessActivityId)
          .forEach(completed::add);
      Set<String> taskNodes = new HashSet<>();
      siblings.stream().map(AgentTask::getProcessActivityId).forEach(taskNodes::add);
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
          if (taskNodes.contains(source)) predecessors.add(source);
          else queue.addLast(source);
        }
      }
      return completed.containsAll(predecessors);
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
