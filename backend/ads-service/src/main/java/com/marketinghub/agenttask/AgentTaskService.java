package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: coordenar a caixa de entrada e o ciclo de vida das tarefas dos agentes. */
@Service
public class AgentTaskService {
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
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Configura persistência, catálogo e relógio operacional. */
  @Autowired
  public AgentTaskService(
      AgentTaskRepository repository,
      AgentRepository agentRepository,
      BusinessProcessDefinitionRepository processRepository,
      ObjectMapper objectMapper) {
    this(repository, agentRepository, processRepository, objectMapper, Clock.systemUTC());
  }

  /** Permite testes determinísticos do histórico temporal. */
  AgentTaskService(
      AgentTaskRepository repository,
      AgentRepository agentRepository,
      BusinessProcessDefinitionRepository processRepository,
      ObjectMapper objectMapper,
      Clock clock) {
    this.repository = repository;
    this.agentRepository = agentRepository;
    this.processRepository = processRepository;
    this.objectMapper = objectMapper;
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
        task.getReceivedAt(),
        task.getDeliveredAt(),
        task.getCreatedAt(),
        task.getUpdatedAt());
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
          if (!owner.isEmpty()
              && !owner
                  .toLowerCase(Locale.ROOT)
                  .contains(assignee.getNickname().toLowerCase(Locale.ROOT))
              && !owner
                  .toLowerCase(Locale.ROOT)
                  .contains(assignee.getAgentKey().toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A atividade selecionada pertence a outro responsável.");
          }
          return new ProcessBinding(
              definition, node.path("id").asText(), node.path("label").asText(), false, null);
        }
      }
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("Não foi possível validar o processo da tarefa.", ex);
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Atividade do processo não encontrada.");
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
    agent(agentKey);
    for (AgentTask task :
        repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            agentKey.trim(), "WORK", "PENDING")) {
      if (task.getProcessDefinition() == null || !predecessorsCompleted(task)) continue;
      Instant now = Instant.now(clock);
      task.setStatus("IN_PROGRESS");
      if (task.getReceivedAt() == null) task.setReceivedAt(now);
      task.setUpdatedAt(now);
      repository.save(task);
      BusinessProcessDefinition process = task.getProcessDefinition();
      return Optional.of(
          new AgentTaskPendingResponse(
              task.getId(),
              agentKey.trim(),
              process.getProcessCode(),
              process.getVersionNumber(),
              task.getProcessActivityId(),
              task.getProcessActivityName(),
              task.getTitle(),
              task.getDescription(),
              task.getSourceReference(),
              task.getReceivedAt()));
    }
    return Optional.empty();
  }

  /** Conclui trabalho reservado com saída e evidências persistidas. */
  @Transactional
  public void completeClaimedProcessTask(
      String agentKey, Long taskId, CompleteAgentTaskRequest request) {
    AgentTask task = claimedBy(agentKey, taskId);
    Instant now = Instant.now(clock);
    task.setResultJson(request.resultJson());
    task.setEvidenceJson(request.evidenceJson());
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
    task.setStatus("BLOCKED");
    task.setUpdatedAt(Instant.now(clock));
    repository.save(task);
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
      for (JsonNode edge : diagram.path("edges")) {
        incoming
            .computeIfAbsent(edge.path("target").asText(), ignored -> new ArrayList<>())
            .add(edge.path("source").asText());
      }
      List<AgentTask> siblings =
          repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
              candidate.getProcessDefinition().getId(), candidate.getSourceReference());
      Set<String> completed = new HashSet<>();
      siblings.stream()
          .filter(task -> "COMPLETED".equals(task.getStatus()))
          .map(AgentTask::getProcessActivityId)
          .forEach(completed::add);
      Set<String> taskNodes = new HashSet<>();
      for (JsonNode node : diagram.path("nodes")) {
        if ("TASK".equals(node.path("type").asText())) taskNodes.add(node.path("id").asText());
      }
      Set<String> predecessors = new HashSet<>();
      Set<String> visited = new HashSet<>();
      ArrayDeque<String> queue = new ArrayDeque<>();
      queue.add(candidate.getProcessActivityId());
      while (!queue.isEmpty()) {
        String current = queue.removeFirst();
        for (String source : incoming.getOrDefault(current, List.of())) {
          if (!visited.add(source)) continue;
          if (taskNodes.contains(source)) predecessors.add(source);
          else queue.addLast(source);
        }
      }
      return completed.containsAll(predecessors);
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Não foi possível avaliar a sequência BPM da tarefa " + candidate.getId(), ex);
    }
  }
}
