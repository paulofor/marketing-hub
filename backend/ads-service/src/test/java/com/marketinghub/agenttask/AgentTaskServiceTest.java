package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar autoria, segregação e ciclo de vida das tarefas dos agentes. */
class AgentTaskServiceTest {

  /** Reutiliza a atividade oficial existente quando a tela repete o mesmo comando. */
  @Test
  void reusesExistingHumanTaskForTheSameExecution() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    Agent dedalo = agent(7L, "landing-generator", "Dédalo");
    AgentTask existing =
        processTask(601L, dedalo, process("PUBLISHED", "Dédalo"), "html", "PENDING");
    existing.setSourceReference("experiment:88");
    when(repository.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:88"))
        .thenReturn(List.of(existing));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    AgentTaskResponse result =
        service.createByHumanIfAbsent(regularActivityRequest("Montar landing"));

    assertThat(result.id()).isEqualTo(601L);
    assertThat(result.status()).isEqualTo("PENDING");
    verify(repository, never()).save(any());
  }

  /** Distingue etapas do mesmo agente e da mesma convergência ao aplicar idempotência. */
  @Test
  void reusesOnlyTheMatchingOperationalActivity() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    Agent iris = agent(12L, "communication-director", "Íris");
    BusinessProcessDefinition landingProcess = process("PUBLISHED", "Íris");
    AgentTask select = processTask(701L, iris, landingProcess, "select", "PENDING");
    AgentTask html = processTask(704L, iris, landingProcess, "html", "PENDING");
    select.setSourceReference("commercial-plan:4@v3:convergence:14");
    html.setSourceReference("commercial-plan:4@v3:convergence:14");
    when(repository.findBySourceReferenceOrderByCreatedAtAscIdAsc(
            "commercial-plan:4@v3:convergence:14"))
        .thenReturn(List.of(select, html));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    AgentTaskResponse result =
        service.createOperationalDelegationIfAbsent(
            new CreateAgentTaskByAgentRequest(
                "meta-ad-approver",
                "communication-director",
                "Materializar correção",
                "Aplicar somente a correção aprovada.",
                "HIGH",
                "commercial-plan:4@v3:convergence:14",
                9L,
                "html",
                false,
                null));

    assertThat(result.id()).isEqualTo(704L);
    assertThat(result.processActivityId()).isEqualTo("html");
    verify(repository, never()).save(any());
  }

  /** Preserva a tarefa do ciclo quando uma versão nova renomeia sua atividade equivalente. */
  @Test
  void reusesCompatibleActivityFromPreviousProcessVersion() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    Agent argos = agent(8L, "market-radar", "Argos");
    BusinessProcessDefinition previous = process("RETIRED", "Argos");
    previous.setId(48L);
    previous.setProcessCode("pde-opportunity-discovery");
    AgentTask existing = processTask(901L, argos, previous, "evidence", "PENDING");
    existing.setSourceReference("product-discovery-cycle:37");
    BusinessProcessDefinition current = process("PUBLISHED", "Argos");
    current.setId(49L);
    current.setProcessCode("pde-opportunity-discovery");
    current.setDiagramJson(
        "{\"nodes\":[{\"id\":\"inspiration\",\"type\":\"TASK\","
            + "\"label\":\"Qualificar fontes\",\"owner\":\"Argos\"}],\"edges\":[]}");
    when(agents.findByAgentKey("market-radar")).thenReturn(Optional.of(argos));
    when(processes.findById(49L)).thenReturn(Optional.of(current));
    when(repository.findBySourceReferenceOrderByCreatedAtAscIdAsc("product-discovery-cycle:37"))
        .thenReturn(List.of(existing));
    AgentTaskService service =
        new AgentTaskService(repository, agents, processes, new ObjectMapper(), Clock.systemUTC());

    AgentTaskResponse result =
        service.createByHumanIfAbsentAcrossProcessVersions(
            new CreateAgentTaskRequest(
                "market-radar",
                "Marketing Hub",
                "Pesquisar oportunidade PDE #37",
                "Qualificar fontes.",
                "HIGH",
                "product-discovery-cycle:37",
                49L,
                "inspiration",
                false,
                null),
            List.of("inspiration", "evidence"));

    assertThat(result.id()).isEqualTo(901L);
    assertThat(result.processDefinitionId()).isEqualTo(48L);
    assertThat(result.processActivityId()).isEqualTo("evidence");
    verify(repository, never()).save(any());
  }

  /** Cria a primeira tarefa na versão atual quando o ciclo ainda não possui correlação. */
  @Test
  void createsTaskInCurrentVersionWhenProcessHistoryIsEmpty() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    Agent argos = agent(8L, "market-radar", "Argos");
    BusinessProcessDefinition current = process("PUBLISHED", "Argos");
    current.setId(49L);
    current.setProcessCode("pde-opportunity-discovery");
    current.setDiagramJson(
        "{\"nodes\":[{\"id\":\"inspiration\",\"type\":\"TASK\","
            + "\"label\":\"Qualificar fontes\",\"owner\":\"Argos\"}],\"edges\":[]}");
    when(agents.findByAgentKey("market-radar")).thenReturn(Optional.of(argos));
    when(processes.findById(49L)).thenReturn(Optional.of(current));
    when(repository.findBySourceReferenceOrderByCreatedAtAscIdAsc("product-discovery-cycle:43"))
        .thenReturn(List.of());
    when(repository.save(any(AgentTask.class)))
        .thenAnswer(
            invocation -> {
              AgentTask task = invocation.getArgument(0);
              task.setId(902L);
              return task;
            });
    AgentTaskService service =
        new AgentTaskService(repository, agents, processes, new ObjectMapper(), Clock.systemUTC());

    AgentTaskResponse result =
        service.createByHumanIfAbsentAcrossProcessVersions(
            new CreateAgentTaskRequest(
                "market-radar",
                "Marketing Hub",
                "Pesquisar oportunidade PDE #43",
                "Qualificar fontes.",
                "HIGH",
                "product-discovery-cycle:43",
                49L,
                "inspiration",
                false,
                null),
            List.of("inspiration", "evidence"));

    assertThat(result.id()).isEqualTo(902L);
    assertThat(result.processDefinitionId()).isEqualTo(49L);
    assertThat(result.processActivityId()).isEqualTo("inspiration");
    verify(repository).save(any(AgentTask.class));
  }

  /** Cria nova tentativa para o revisor bloqueado sem duplicar outra tarefa ainda pendente. */
  @Test
  void retriesBlockedHumanReviewAndRefreshesPendingContext() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    BusinessProcessDefinition process = process("PUBLISHED", "Psique");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"html\",\"type\":\"TASK\",\"label\":\"Montar HTML\","
            + "\"owner\":\"Psique\"},{\"id\":\"customer\",\"type\":\"TASK\","
            + "\"label\":\"Avaliar percepção\",\"owner\":\"Psique\"}]}");
    AgentTask blocked = processTask(601L, psique, process, "customer", "BLOCKED");
    blocked.setSourceReference("commercial-plan:4@v3:journey");
    AtomicReference<AgentTask> saved = new AtomicReference<>();
    when(repository.findBySourceReferenceOrderByCreatedAtAscIdAsc("commercial-plan:4@v3:journey"))
        .thenReturn(List.of(blocked));
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(processes.findById(9L)).thenReturn(Optional.of(process));
    when(repository.save(any(AgentTask.class)))
        .thenAnswer(
            invocation -> {
              AgentTask task = invocation.getArgument(0);
              if (task.getId() == null) task.setId(602L);
              saved.set(task);
              return task;
            });
    AgentTaskService service =
        new AgentTaskService(repository, agents, processes, new ObjectMapper(), Clock.systemUTC());
    CreateAgentTaskRequest request =
        new CreateAgentTaskRequest(
            "customer-agent",
            "Operador",
            "Retomar avaliação",
            "Snapshot canônico atualizado.",
            "HIGH",
            "commercial-plan:4@v3:journey",
            9L,
            "customer",
            false,
            null);

    AgentTaskResponse retry = service.retryBlockedByHumanOrRefreshPending(request);

    assertThat(retry.id()).isEqualTo(602L);
    assertThat(retry.status()).isEqualTo("PENDING");
    assertThat(saved.get().getDescription()).isEqualTo("Snapshot canônico atualizado.");

    AgentTask pending = saved.get();
    when(repository.findBySourceReferenceOrderByCreatedAtAscIdAsc("commercial-plan:4@v3:journey"))
        .thenReturn(List.of(blocked, pending));
    request =
        new CreateAgentTaskRequest(
            "customer-agent",
            "Operador",
            "Retomar avaliação",
            "Snapshot canônico mais recente.",
            "HIGH",
            "commercial-plan:4@v3:journey",
            9L,
            "customer",
            false,
            null);

    AgentTaskResponse reused = service.retryBlockedByHumanOrRefreshPending(request);

    assertThat(reused.id()).isEqualTo(602L);
    assertThat(saved.get().getDescription()).isEqualTo("Snapshot canônico mais recente.");
  }

  /** Persiste atividade, instância e tentativas como níveis distintos do mesmo trabalho. */
  @Test
  void persistsActivityInstanceAndGroupsItsAttempts() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    BusinessProcessActivityInstanceRepository instances =
        mock(BusinessProcessActivityInstanceRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    BusinessProcessActivityDefinitionRepository activities =
        mock(BusinessProcessActivityDefinitionRepository.class);
    Agent dedalo = agent(7L, "landing-generator", "Dédalo");
    BusinessProcessDefinition process = process("PUBLISHED", "Dédalo");
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(401L);
    activity.setProcessDefinition(process);
    activity.setActivityId("html");
    activity.setName("Montar HTML");
    activity.setObjective("Entregar HTML funcional e responsivo.");
    AtomicReference<AgentTask> savedTask = new AtomicReference<>();
    AtomicReference<BusinessProcessActivityInstance> savedInstance = new AtomicReference<>();
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(dedalo));
    when(processes.findById(9L)).thenReturn(Optional.of(process));
    when(activities.findByProcessDefinitionIdAndActivityId(9L, "html"))
        .thenReturn(Optional.of(activity));
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            401L, "experiment:88"))
        .thenReturn(Optional.empty());
    when(instances.save(any(BusinessProcessActivityInstance.class)))
        .thenAnswer(
            invocation -> {
              BusinessProcessActivityInstance value = invocation.getArgument(0);
              if (value.getId() == null) value.setId(501L);
              savedInstance.set(value);
              return value;
            });
    when(repository.save(any(AgentTask.class)))
        .thenAnswer(
            invocation -> {
              AgentTask value = invocation.getArgument(0);
              if (value.getId() == null) value.setId(601L);
              savedTask.set(value);
              return value;
            });
    when(repository.findById(601L)).thenAnswer(ignored -> Optional.ofNullable(savedTask.get()));
    when(repository.findByActivityInstanceIdOrderByCreatedAtAscIdAsc(501L))
        .thenAnswer(ignored -> savedTask.get() == null ? List.of() : List.of(savedTask.get()));
    when(repository.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:88"))
        .thenAnswer(ignored -> savedTask.get() == null ? List.of() : List.of(savedTask.get()));
    Instant now = Instant.parse("2026-08-25T18:00:00Z");
    AgentTaskService service =
        new AgentTaskService(
            repository,
            instances,
            agents,
            processes,
            activities,
            null,
            new ObjectMapper(),
            null,
            Clock.fixed(now, ZoneOffset.UTC));

    AgentTaskResponse created =
        service.createByHuman(
            new CreateAgentTaskRequest(
                "landing-generator",
                "Operador",
                "Montar landing",
                "Entregar HTML responsivo.",
                "HIGH",
                "experiment:88",
                9L,
                "html",
                false,
                null));
    service.updateStatus(created.id(), new UpdateAgentTaskStatusRequest("IN_PROGRESS"));
    service.updateStatus(created.id(), new UpdateAgentTaskStatusRequest("COMPLETED"));

    BusinessProcessActivityInstance instance = savedInstance.get();
    assertThat(savedTask.get().getActivityInstance()).isSameAs(instance);
    assertThat(savedTask.get().getReceivedAt()).isEqualTo(now);
    assertThat(instance.getStatus()).isEqualTo("COMPLETED");
    assertThat(instance.isObjectiveAchieved()).isTrue();
    assertThat(instance.getEnteredAt()).isEqualTo(now);
    assertThat(instance.getExitedAt()).isEqualTo(now);
    ProcessInstanceResponse processInstance = service.processInstances("experiment:88").getFirst();
    assertThat(processInstance.activities()).hasSize(1);
    assertThat(processInstance.activities().getFirst().activityName()).isEqualTo("Montar HTML");
    assertThat(processInstance.activities().getFirst().objective())
        .isEqualTo("Entregar HTML funcional e responsivo.");
    assertThat(processInstance.activities().getFirst().tasks()).hasSize(1);
    assertThat(processInstance.activities().getFirst().tasks().getFirst().attemptNumber()).isOne();
  }

  /** Conclui a ocorrência conjunta somente depois dos pareceres de Psique e Têmis. */
  @Test
  void completesCoauthoredActivityOnlyAfterEveryAgentFinishes() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    BusinessProcessActivityInstanceRepository instances =
        mock(BusinessProcessActivityInstanceRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    BusinessProcessActivityDefinitionRepository activities =
        mock(BusinessProcessActivityDefinitionRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    Agent themis = agent(6L, "meta-ad-approver", "Têmis");
    BusinessProcessDefinition process = process("PUBLISHED", "Psique e Têmis");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"html\",\"type\":\"TASK\",\"label\":\"Validar PDE\","
            + "\"owner\":\"Psique e Têmis\",\"responsibleAgentKeys\":[\"customer-agent\","
            + "\"meta-ad-approver\"]}]}");
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(401L);
    activity.setProcessDefinition(process);
    activity.setActivityId("html");
    activity.setName("Validar PDE");
    activity.setObjective("Comprovar fatos, controle e valor.");
    List<AgentTask> savedTasks = new ArrayList<>();
    AtomicReference<BusinessProcessActivityInstance> savedInstance = new AtomicReference<>();
    AtomicLong taskSequence = new AtomicLong(600L);
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(agents.findByAgentKey("meta-ad-approver")).thenReturn(Optional.of(themis));
    when(processes.findById(9L)).thenReturn(Optional.of(process));
    when(activities.findByProcessDefinitionIdAndActivityId(9L, "html"))
        .thenReturn(Optional.of(activity));
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            401L, "experiment:90"))
        .thenAnswer(ignored -> Optional.ofNullable(savedInstance.get()));
    when(instances.save(any(BusinessProcessActivityInstance.class)))
        .thenAnswer(
            invocation -> {
              BusinessProcessActivityInstance value = invocation.getArgument(0);
              if (value.getId() == null) value.setId(501L);
              savedInstance.set(value);
              return value;
            });
    when(repository.save(any(AgentTask.class)))
        .thenAnswer(
            invocation -> {
              AgentTask task = invocation.getArgument(0);
              if (task.getId() == null) {
                task.setId(taskSequence.incrementAndGet());
                savedTasks.add(task);
              }
              return task;
            });
    when(repository.findById(any(Long.class)))
        .thenAnswer(
            invocation ->
                savedTasks.stream()
                    .filter(task -> task.getId().equals(invocation.getArgument(0)))
                    .findFirst());
    when(repository.findByActivityInstanceIdOrderByCreatedAtAscIdAsc(501L))
        .thenAnswer(ignored -> List.copyOf(savedTasks));
    Instant now = Instant.parse("2026-08-27T22:00:00Z");
    AgentTaskService service =
        new AgentTaskService(
            repository,
            instances,
            agents,
            processes,
            activities,
            null,
            new ObjectMapper(),
            null,
            Clock.fixed(now, ZoneOffset.UTC));

    AgentTaskResponse psiqueTask = service.createByHuman(coauthorRequest("customer-agent"));
    AgentTaskResponse themisTask = service.createByHuman(coauthorRequest("meta-ad-approver"));
    service.updateStatus(psiqueTask.id(), new UpdateAgentTaskStatusRequest("IN_PROGRESS"));
    service.updateStatus(psiqueTask.id(), new UpdateAgentTaskStatusRequest("COMPLETED"));

    assertThat(savedInstance.get().getStatus()).isEqualTo("PENDING");
    assertThat(savedInstance.get().isObjectiveAchieved()).isFalse();

    service.updateStatus(themisTask.id(), new UpdateAgentTaskStatusRequest("IN_PROGRESS"));
    service.updateStatus(themisTask.id(), new UpdateAgentTaskStatusRequest("COMPLETED"));

    assertThat(savedInstance.get().getStatus()).isEqualTo("COMPLETED");
    assertThat(savedInstance.get().isObjectiveAchieved()).isTrue();
    assertThat(savedTasks)
        .extracting(task -> task.getAssignedAgent().getAgentKey())
        .containsExactly("customer-agent", "meta-ad-approver");
  }

  /** Substitui o bloqueio pela correção mais recente e preserva um ciclo já encerrado. */
  @Test
  void resolvesRetryAndOpensAnotherOccurrenceAfterCompletion() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    BusinessProcessActivityInstanceRepository instances =
        mock(BusinessProcessActivityInstanceRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    BusinessProcessActivityDefinitionRepository activities =
        mock(BusinessProcessActivityDefinitionRepository.class);
    Agent dedalo = agent(7L, "landing-generator", "Dédalo");
    BusinessProcessDefinition process = process("PUBLISHED", "Dédalo");
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(401L);
    activity.setProcessDefinition(process);
    activity.setActivityId("html");
    activity.setName("Montar HTML");
    List<AgentTask> savedTasks = new ArrayList<>();
    List<BusinessProcessActivityInstance> savedInstances = new ArrayList<>();
    AtomicLong taskSequence = new AtomicLong(600L);
    AtomicLong instanceSequence = new AtomicLong(500L);
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(dedalo));
    when(processes.findById(9L)).thenReturn(Optional.of(process));
    when(activities.findByProcessDefinitionIdAndActivityId(9L, "html"))
        .thenReturn(Optional.of(activity));
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            401L, "experiment:88"))
        .thenAnswer(
            ignored ->
                savedInstances.isEmpty()
                    ? Optional.empty()
                    : Optional.of(savedInstances.getLast()));
    when(instances.save(any(BusinessProcessActivityInstance.class)))
        .thenAnswer(
            invocation -> {
              BusinessProcessActivityInstance value = invocation.getArgument(0);
              if (value.getId() == null) {
                value.setId(instanceSequence.incrementAndGet());
                savedInstances.add(value);
              }
              return value;
            });
    when(repository.save(any(AgentTask.class)))
        .thenAnswer(
            invocation -> {
              AgentTask value = invocation.getArgument(0);
              if (value.getId() == null) {
                value.setId(taskSequence.incrementAndGet());
                savedTasks.add(value);
              }
              return value;
            });
    when(repository.findById(any(Long.class)))
        .thenAnswer(
            invocation ->
                savedTasks.stream()
                    .filter(task -> task.getId().equals(invocation.getArgument(0)))
                    .findFirst());
    when(repository.findByActivityInstanceIdOrderByCreatedAtAscIdAsc(any(Long.class)))
        .thenAnswer(
            invocation ->
                savedTasks.stream()
                    .filter(
                        task ->
                            task.getActivityInstance().getId().equals(invocation.getArgument(0)))
                    .toList());
    AgentTaskService service =
        new AgentTaskService(
            repository,
            instances,
            agents,
            processes,
            activities,
            null,
            new ObjectMapper(),
            null,
            Clock.fixed(Instant.parse("2026-08-25T18:00:00Z"), ZoneOffset.UTC));

    AgentTaskResponse first = service.createByHuman(regularActivityRequest("Primeira tentativa"));
    service.updateStatus(first.id(), new UpdateAgentTaskStatusRequest("IN_PROGRESS"));
    service.updateStatus(first.id(), new UpdateAgentTaskStatusRequest("BLOCKED"));
    AgentTaskResponse retry = service.createByHuman(regularActivityRequest("Correção"));
    service.updateStatus(retry.id(), new UpdateAgentTaskStatusRequest("IN_PROGRESS"));
    service.updateStatus(retry.id(), new UpdateAgentTaskStatusRequest("COMPLETED"));

    assertThat(savedInstances).hasSize(1);
    assertThat(savedInstances.getFirst().getStatus()).isEqualTo("COMPLETED");
    assertThat(savedInstances.getFirst().isObjectiveAchieved()).isTrue();
    service.createByHuman(regularActivityRequest("Novo ciclo"));
    assertThat(savedInstances).hasSize(2);
    assertThat(savedInstances.getFirst().getStatus()).isEqualTo("COMPLETED");
    assertThat(savedInstances.getLast().getOccurrenceNumber()).isEqualTo(2);
    assertThat(savedTasks.getLast().getActivityInstance()).isSameAs(savedInstances.getLast());
  }

  /** Encerra tarefas reabríveis quando a entidade comercial não pode mais avançar. */
  @Test
  void cancelsActiveTasksForTerminalExperiment() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask pending = new AgentTask();
    pending.setId(188L);
    pending.setStatus("PENDING");
    pending.setCreatedAt(Instant.parse("2026-08-22T22:33:54Z"));
    pending.setUpdatedAt(pending.getCreatedAt());
    AgentTask completed = new AgentTask();
    completed.setId(187L);
    completed.setStatus("COMPLETED");
    completed.setCreatedAt(Instant.parse("2026-08-22T22:24:33Z"));
    completed.setUpdatedAt(completed.getCreatedAt());
    when(repository.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:88"))
        .thenReturn(List.of(completed, pending));
    Clock clock = Clock.fixed(Instant.parse("2026-08-23T01:45:00Z"), ZoneOffset.UTC);
    AgentTaskService service =
        new AgentTaskService(
            repository,
            mock(AgentRepository.class),
            mock(BusinessProcessDefinitionRepository.class),
            new ObjectMapper(),
            clock);

    int cancelled =
        service.cancelActiveTasksBySourceReference(
            "experiment:88", "Trava financeira encerrada sem resultado primário.");

    assertThat(cancelled).isEqualTo(1);
    assertThat(pending.getStatus()).isEqualTo("CANCELLED");
    assertThat(pending.getExecutionError())
        .isEqualTo("Trava financeira encerrada sem resultado primário.");
    assertThat(pending.getUpdatedAt()).isEqualTo(Instant.parse("2026-08-23T01:45:00Z"));
    assertThat(completed.getStatus()).isEqualTo("COMPLETED");
    verify(repository).saveAll(List.of(pending));
  }

  /** Vincula a nova tarefa humana a uma atividade publicada do responsável correto. */
  @Test
  void createsHumanTaskBoundToPublishedActivity() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    Agent dedalo = agent(7L, "landing-generator", "Dédalo");
    BusinessProcessDefinition process = process("PUBLISHED", "Dédalo");
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(dedalo));
    when(processes.findById(9L)).thenReturn(Optional.of(process));
    when(repository.save(any(AgentTask.class)))
        .thenAnswer(
            invocation -> {
              AgentTask task = invocation.getArgument(0);
              task.setId(71L);
              return task;
            });
    AgentTaskService service =
        new AgentTaskService(repository, agents, processes, new ObjectMapper(), Clock.systemUTC());

    AgentTaskResponse response =
        service.createByHuman(
            new CreateAgentTaskRequest(
                "landing-generator",
                "Operador",
                "Montar landing",
                "Entregar HTML responsivo.",
                "HIGH",
                "experiment:88",
                9L,
                "html",
                false,
                null));

    assertThat(response.processDefinitionId()).isEqualTo(9L);
    assertThat(response.processActivityName()).isEqualTo("Montar HTML");
    assertThat(response.exceptional()).isFalse();
  }

  /** Vincula Hermes à atividade cujo responsável usa seu nome funcional no BPM publicado. */
  @Test
  void createsHumanTaskBoundByAgentFunctionalName() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    Agent hermes = agent(1L, "growth-operator", "Hermes");
    hermes.setName("Operador de Crescimento");
    BusinessProcessDefinition process = process("PUBLISHED", "Operador de Crescimento");
    when(agents.findByAgentKey("growth-operator")).thenReturn(Optional.of(hermes));
    when(processes.findById(9L)).thenReturn(Optional.of(process));
    when(repository.save(any(AgentTask.class)))
        .thenAnswer(
            invocation -> {
              AgentTask task = invocation.getArgument(0);
              task.setId(72L);
              return task;
            });
    AgentTaskService service =
        new AgentTaskService(repository, agents, processes, new ObjectMapper(), Clock.systemUTC());

    AgentTaskResponse response =
        service.createByHuman(
            new CreateAgentTaskRequest(
                "growth-operator",
                "Usuário do Marketing Hub",
                "Verificar integridade dos eventos",
                "Confirmar visita, checkout, venda e entrega sem duplicidade.",
                "HIGH",
                "experiment:88",
                9L,
                "html",
                false,
                null));

    assertThat(response.assignedAgentKey()).isEqualTo("growth-operator");
    assertThat(response.sourceReference()).isEqualTo("experiment:88");
    assertThat(response.processActivityName()).isEqualTo("Montar HTML");
  }

  /** Aceita somente as identidades técnicas declaradas para uma atividade de coautoria. */
  @Test
  void validatesEveryDeclaredCoauthorByAgentKey() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    Agent themis = agent(6L, "meta-ad-approver", "Têmis");
    Agent dedalo = agent(7L, "landing-generator", "Dédalo");
    BusinessProcessDefinition process = process("PUBLISHED", "Psique e Têmis");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"html\",\"type\":\"TASK\",\"label\":\"Validar PDE\","
            + "\"owner\":\"Revisão conjunta\",\"responsibleAgentKeys\":[\"customer-agent\","
            + "\"meta-ad-approver\"]}]}");
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(agents.findByAgentKey("meta-ad-approver")).thenReturn(Optional.of(themis));
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(dedalo));
    when(processes.findById(9L)).thenReturn(Optional.of(process));
    AtomicLong sequence = new AtomicLong(80L);
    when(repository.save(any(AgentTask.class)))
        .thenAnswer(
            invocation -> {
              AgentTask task = invocation.getArgument(0);
              task.setId(sequence.incrementAndGet());
              return task;
            });
    AgentTaskService service =
        new AgentTaskService(repository, agents, processes, new ObjectMapper(), Clock.systemUTC());

    AgentTaskResponse psiqueTask = service.createByHuman(coauthorRequest("customer-agent"));
    AgentTaskResponse themisTask = service.createByHuman(coauthorRequest("meta-ad-approver"));

    assertThat(psiqueTask.assignedAgentKey()).isEqualTo("customer-agent");
    assertThat(themisTask.assignedAgentKey()).isEqualTo("meta-ad-approver");
    assertThatThrownBy(() -> service.createByHuman(coauthorRequest("landing-generator")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("outro responsável");
  }

  /** Importa um parecer real já concluído com evidência e consumo no vínculo BPM publicado. */
  @Test
  void recordsImportedCompletedReview() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    BusinessProcessDefinition process = process("PUBLISHED", "Psique");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"customer\",\"type\":\"TASK\",\"label\":\"Avaliar percepção\",\"owner\":\"Psique\"}]}");
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(processes.findById(9L)).thenReturn(Optional.of(process));
    AtomicReference<AgentTask> saved = new AtomicReference<>();
    when(repository.save(any(AgentTask.class)))
        .thenAnswer(
            invocation -> {
              AgentTask task = invocation.getArgument(0);
              task.setId(72L);
              saved.set(task);
              return task;
            });
    when(repository.findById(72L)).thenAnswer(ignored -> Optional.ofNullable(saved.get()));
    Instant now = Instant.parse("2026-08-25T12:00:00Z");
    AgentTaskService service =
        new AgentTaskService(
            repository,
            agents,
            processes,
            new ObjectMapper(),
            null,
            Clock.fixed(now, ZoneOffset.UTC));

    AgentTaskResponse response =
        service.recordImportedCompletedTask(
            new ImportedCompletedAgentTask(
                "customer-agent",
                "Operador humano",
                "Parecer importado",
                "Preserva o parecer local.",
                "commercial-plan:4@v3",
                9L,
                "customer",
                "{\"decision\":\"APPROVED\"}",
                "{\"creativePackageId\":\"abc\"}",
                List.of(new AgentTaskModelUsageRequest("gpt-5.6-sol", "STANDARD", 100L, 20L, 10L)),
                new AgentTaskExecutionAuditRequest(
                    "MODEL",
                    "gpt-5.6-sol",
                    "high",
                    "Núcleo de Psique.\n\nAvalie o pacote importado.",
                    "Núcleo de Psique.",
                    "Avalie o pacote importado.",
                    List.of())));

    assertThat(response.status()).isEqualTo("COMPLETED");
    assertThat(response.resultJson()).contains("APPROVED");
    assertThat(response.evidenceJson()).contains("creativePackageId");
    assertThat(response.inputTokens()).isEqualTo(100L);
    assertThat(response.receivedAt()).isEqualTo(now);
    assertThat(response.deliveredAt()).isEqualTo(now);
  }

  /** Vincula uma tarefa excepcional pendente sem perder sua identidade e seu histórico. */
  @Test
  void bindsPendingExceptionalTaskToPublishedActivity() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    Agent dedalo = agent(7L, "landing-generator", "Dédalo");
    BusinessProcessDefinition process = process("PUBLISHED", "Dédalo");
    AgentTask task = new AgentTask();
    task.setId(40L);
    task.setAssignedAgent(dedalo);
    task.setRequestedByType("HUMAN");
    task.setRequestedByName("Operador de Crescimento");
    task.setTitle("Cadastrar exemplos reais");
    task.setDescription("Cadastrar referências auditáveis.");
    task.setPriority("HIGH");
    task.setStatus("PENDING");
    task.setTaskKind("WORK");
    task.setExceptional(true);
    task.setExceptionReason("Processo ainda não possuía a atividade.");
    task.setCreatedAt(Instant.parse("2026-08-15T21:30:22Z"));
    task.setUpdatedAt(task.getCreatedAt());
    when(repository.findById(40L)).thenReturn(Optional.of(task));
    when(processes.findById(9L)).thenReturn(Optional.of(process));
    when(repository.save(task)).thenReturn(task);
    AgentTaskService service =
        new AgentTaskService(
            repository,
            mock(AgentRepository.class),
            processes,
            new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-08-15T21:40:00Z"), ZoneOffset.UTC));

    AgentTaskResponse response =
        service.bindProcess(40L, new BindAgentTaskProcessRequest(9L, "html"));

    assertThat(response.id()).isEqualTo(40L);
    assertThat(response.processDefinitionId()).isEqualTo(9L);
    assertThat(response.processActivityName()).isEqualTo("Montar HTML");
    assertThat(response.exceptional()).isFalse();
    assertThat(response.exceptionReason()).isNull();
  }

  /** Impede migrar tarefa que um executor já recebeu. */
  @Test
  void rejectsBindingAfterTaskWasReceived() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task = new AgentTask();
    task.setId(40L);
    task.setExceptional(true);
    task.setStatus("IN_PROGRESS");
    task.setReceivedAt(Instant.parse("2026-08-15T21:35:00Z"));
    when(repository.findById(40L)).thenReturn(Optional.of(task));

    assertThatThrownBy(
            () ->
                service(repository, mock(AgentRepository.class), Clock.systemUTC())
                    .bindProcess(40L, new BindAgentTaskProcessRequest(9L, "html")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("pendente e ainda não recebida");
    verify(repository, never()).save(any());
  }

  /** Bloqueia tarefa sem processo quando ela não foi declarada como exceção. */
  @Test
  void rejectsHumanTaskWithoutProcess() {
    AgentRepository agents = mock(AgentRepository.class);
    when(agents.findByAgentKey("landing-generator"))
        .thenReturn(Optional.of(agent(7L, "landing-generator", "Dédalo")));
    AgentTaskService service = service(mock(AgentTaskRepository.class), agents, Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.createByHuman(
                    new CreateAgentTaskRequest(
                        "landing-generator",
                        "Operador",
                        "Montar landing",
                        "Entregar HTML.",
                        "HIGH",
                        null,
                        null,
                        null,
                        false,
                        null)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Selecione um processo publicado");
  }

  /** Exige causa auditável para trabalho excepcional fora do catálogo. */
  @Test
  void rejectsExceptionalTaskWithoutReason() {
    AgentRepository agents = mock(AgentRepository.class);
    when(agents.findByAgentKey("landing-generator"))
        .thenReturn(Optional.of(agent(7L, "landing-generator", "Dédalo")));
    AgentTaskService service = service(mock(AgentTaskRepository.class), agents, Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.createByHuman(
                    new CreateAgentTaskRequest(
                        "landing-generator",
                        "Operador",
                        "Incidente",
                        "Corrigir incidente.",
                        "URGENT",
                        null,
                        null,
                        null,
                        true,
                        " ")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("exige justificativa");
  }

  /** Registra delegação entre agentes preservando as duas identidades. */
  @Test
  void createsTaskDelegatedByAnotherAgent() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent requester = agent(1L, "financial-agent", "Plutus");
    Agent assignee = agent(8L, "videomaker", "Apolo");
    when(agents.findByAgentKey("financial-agent")).thenReturn(Optional.of(requester));
    when(agents.findByAgentKey("videomaker")).thenReturn(Optional.of(assignee));
    when(repository.save(any(AgentTask.class)))
        .thenAnswer(
            invocation -> {
              AgentTask task = invocation.getArgument(0);
              task.setId(41L);
              return task;
            });
    Instant now = Instant.parse("2026-08-11T15:00:00Z");
    AgentTaskService service = service(repository, agents, Clock.fixed(now, ZoneOffset.UTC));

    AgentTaskResponse response =
        service.createByAgent(
            new CreateAgentTaskByAgentRequest(
                "financial-agent",
                "videomaker",
                "Produzir vídeo",
                "Produzir dentro do orçamento aprovado.",
                "HIGH",
                "musa-v7"));

    assertThat(response.assignedAgentNickname()).isEqualTo("Apolo");
    assertThat(response.requestedByName()).isEqualTo("Plutus");
    assertThat(response.requestedByType()).isEqualTo("AGENT");
    assertThat(response.exceptional()).isTrue();
    assertThat(response.exceptionReason()).contains("sem atividade BPM");
    assertThat(response.status()).isEqualTo("PENDING");
    assertThat(response.createdAt()).isEqualTo(now);
    assertThat(response.receivedAt()).isNull();
    assertThat(response.deliveredAt()).isNull();
  }

  /** Conclui delegação operacional somente depois de copiar sua auditoria técnica integral. */
  @Test
  void completesOperationalDelegationWithExecutionAudit() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task = new AgentTask();
    task.setId(51L);
    task.setAssignedAgent(agent(7L, "landing-generator", "Dédalo"));
    task.setRequestedByType("AGENT");
    task.setRequestedByName("Backend");
    task.setTitle("Gerar landing");
    task.setDescription("Materializar a candidata aprovada.");
    task.setPriority("HIGH");
    task.setStatus("IN_PROGRESS");
    task.setTaskKind("WORK");
    task.setCreatedAt(Instant.parse("2026-08-29T01:00:00Z"));
    task.setUpdatedAt(task.getCreatedAt());
    when(repository.findTopByAssignedAgentAgentKeyAndSourceReferenceOrderByUpdatedAtDescIdDesc(
            "landing-generator", "landing-cycle:41"))
        .thenReturn(Optional.of(task));
    when(repository.save(task)).thenReturn(task);

    service(repository, mock(AgentRepository.class), Clock.systemUTC())
        .finishOperationalDelegation(
            "landing-generator",
            "landing-cycle:41",
            true,
            new AgentTaskExecutionAuditRequest(
                "MODEL",
                "gpt-5.6-sol",
                "high",
                "Núcleo de Dédalo.\n\nExecute o ciclo 41.",
                "Núcleo de Dédalo.",
                "Execute o ciclo 41.",
                List.of()),
            List.of(new AgentTaskModelUsageRequest("gpt-5.6-sol", "STANDARD", 100L, 20L, 30L)),
            null);

    assertThat(task.getStatus()).isEqualTo("COMPLETED");
    assertThat(task.getExecutionMode()).isEqualTo("MODEL");
    assertThat(task.getExecutionReasoningEffort()).isEqualTo("high");
    assertThat(task.getExecutionPrompt()).isEqualTo("Núcleo de Dédalo.\n\nExecute o ciclo 41.");
    assertThat(task.getExecutionAgentPrompt()).isEqualTo("Núcleo de Dédalo.");
    assertThat(task.getExecutionActivityPrompt()).isEqualTo("Execute o ciclo 41.");
    assertThat(task.getInputTokens()).isEqualTo(100L);
    assertThat(task.getDeliveredAt()).isNotNull();
  }

  /** Registra a entrega no instante da primeira conclusão da tarefa. */
  @Test
  void recordsResultDeliveryWhenTaskCompletes() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task = new AgentTask();
    task.setId(52L);
    task.setAssignedAgent(agent(8L, "videomaker", "Apolo"));
    task.setRequestedByType("HUMAN");
    task.setRequestedByName("Operador");
    task.setTitle("Entregar storyboard");
    task.setDescription("Entregar resultado auditável.");
    task.setPriority("HIGH");
    task.setStatus("IN_PROGRESS");
    task.setTaskKind("WORK");
    Instant receivedAt = Instant.parse("2026-08-15T10:00:00Z");
    Instant deliveredAt = Instant.parse("2026-08-15T10:30:00Z");
    task.setReceivedAt(receivedAt);
    task.setCreatedAt(receivedAt);
    task.setUpdatedAt(receivedAt);
    when(repository.findById(52L)).thenReturn(Optional.of(task));
    when(repository.save(task)).thenReturn(task);
    AgentTaskService service =
        service(repository, mock(AgentRepository.class), Clock.fixed(deliveredAt, ZoneOffset.UTC));

    AgentTaskResponse response =
        service.updateStatus(52L, new UpdateAgentTaskStatusRequest("COMPLETED"));

    assertThat(response.receivedAt()).isEqualTo(receivedAt);
    assertThat(response.deliveredAt()).isEqualTo(deliveredAt);
  }

  /** Registra gate pendente na mesa sem conceder aprovação ao solicitante. */
  @Test
  void createsPendingGateForAnyRegisteredAgent() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent requester = agent(7L, "landing-generator", "Dédalo");
    Agent assignee = agent(6L, "meta-ad-approver", "Têmis");
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(requester));
    when(agents.findByAgentKey("meta-ad-approver")).thenReturn(Optional.of(assignee));
    when(repository.save(any(AgentTask.class)))
        .thenAnswer(
            invocation -> {
              AgentTask task = invocation.getArgument(0);
              task.setId(42L);
              return task;
            });
    when(repository.findById(42L))
        .thenAnswer(
            invocation -> {
              AgentTask task = new AgentTask();
              task.setId(42L);
              task.setAssignedAgent(assignee);
              task.setRequestedByAgent(requester);
              task.setRequestedByType("AGENT");
              task.setRequestedByName("Dédalo");
              task.setTitle("Revisar landing");
              task.setDescription("Validar evidências independentes.");
              task.setPriority("HIGH");
              task.setStatus("PENDING");
              task.setTaskKind("WORK");
              task.setCreatedAt(Instant.now());
              task.setUpdatedAt(task.getCreatedAt());
              return Optional.of(task);
            });
    AgentTaskService service = service(repository, agents, Clock.systemUTC());

    AgentTaskResponse gate =
        service.createGateByAgent(
            new CreateAgentTaskByAgentRequest(
                "landing-generator",
                "meta-ad-approver",
                "Revisar landing",
                "Validar evidências independentes.",
                "HIGH",
                "experiment:88"),
            "LANDING_QUALITY_APPROVAL");

    assertThat(gate.taskKind()).isEqualTo("GATE_DECISION");
    assertThat(gate.gateCode()).isEqualTo("LANDING_QUALITY_APPROVAL");
    assertThat(gate.gateStatus()).isEqualTo("PENDING");
  }

  /** Retorna somente a caixa do destinatário consultado. */
  @Test
  void listsSegregatedInbox() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent assignee = agent(7L, "landing-generator", "Dédalo");
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(assignee));
    when(repository.findByAssignedAgentAgentKeyOrderByCreatedAtDescIdDesc("landing-generator"))
        .thenReturn(List.of());
    AgentTaskService service = service(repository, agents, Clock.systemUTC());

    assertThat(service.inbox("landing-generator")).isEmpty();
  }

  /** Reúne em uma fila central somente tarefas que ainda exigem atuação. */
  @Test
  void listsActiveTasksAcrossAgents() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    Agent assignee = agent(8L, "videomaker", "Apolo");
    AgentTask task = new AgentTask();
    task.setId(91L);
    task.setAssignedAgent(assignee);
    task.setRequestedByType("HUMAN");
    task.setRequestedByName("Operador");
    task.setTitle("Finalizar vídeo MUSA");
    task.setDescription("Concluir montagem e QA.");
    task.setPriority("URGENT");
    task.setStatus("IN_PROGRESS");
    task.setTaskKind("WORK");
    task.setCreatedAt(Instant.parse("2026-08-12T10:00:00Z"));
    task.setUpdatedAt(task.getCreatedAt());
    when(repository.findByStatusInOrderByUpdatedAtDescIdDesc(
            List.of("IN_PROGRESS", "BLOCKED", "PENDING")))
        .thenReturn(List.of(task));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    assertThat(service.activeTasks())
        .singleElement()
        .satisfies(
            result -> {
              assertThat(result.assignedAgentNickname()).isEqualTo("Apolo");
              assertThat(result.status()).isEqualTo("IN_PROGRESS");
            });
  }

  /** Reconstrói falha com intenção, contexto, evidência, saída e limite de autoridade. */
  @Test
  void exposesGovernedFailureAuditWithoutTechnicalLogs() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    Agent assignee = agent(1L, "growth-operator", "Hermes");
    assignee.setAuthorityPolicy("Somente leitura; gasto e publicação exigem aprovação humana.");
    AgentTask task = processTask(92L, assignee, process("PUBLISHED", "Hermes"), "html", "BLOCKED");
    task.setDescription("Confirmar a integridade do funil antes de otimizar.");
    task.setSourceReference("experiment:88");
    task.setGateDecisionReason("Evento de checkout ausente.");
    task.setEvidenceJson("{\"accessMode\":\"READ_ONLY\",\"toolUsage\":[\"consultar_funil\"]}");
    task.setResultJson("{\"decision\":\"BLOCKED\"}");
    task.setBlockerCategory("MISSING_EVIDENCE");
    task.setBlockerAction("Corrigir a integração do evento de checkout e reiniciar a tarefa.");
    AgentTaskAuditLink helpLink = new AgentTaskAuditLink();
    helpLink.setTask(task);
    helpLink.setLinkType("BLOCKER_HELP");
    helpLink.setLabel("Abrir histórico do produto");
    helpLink.setUrl("/products/88/activity-history");
    helpLink.setDisplayOrder(0);
    helpLink.setCreatedAt(Instant.parse("2026-08-12T10:00:00Z"));
    task.getAuditLinks().add(helpLink);
    when(repository.findByStatusInOrderByUpdatedAtDescIdDesc(
            List.of("IN_PROGRESS", "BLOCKED", "PENDING")))
        .thenReturn(List.of(task));

    AgentTaskFailureAuditResponse audit =
        service(repository, mock(AgentRepository.class), Clock.systemUTC())
            .activeTasks()
            .getFirst()
            .failureAudit();

    assertThat(audit.readiness()).isEqualTo("COMPLETE");
    assertThat(audit.intendedWork()).contains("integridade do funil");
    assertThat(audit.sourceReference()).isEqualTo("experiment:88");
    assertThat(audit.processCode()).isEqualTo("landing-page-generation");
    assertThat(audit.accessedEvidenceJson()).contains("consultar_funil");
    assertThat(audit.producedOutputJson()).contains("BLOCKED");
    assertThat(audit.authorityPolicy()).contains("aprovação humana");
    assertThat(audit.missingEvidence()).isEmpty();
  }

  /** Marca como parcial um bloqueio legado que não preservou causa nem acessos. */
  @Test
  void exposesMissingEvidenceInLegacyFailureAudit() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task = new AgentTask();
    task.setId(93L);
    task.setAssignedAgent(agent(3L, "financial-agent", "Plutus"));
    task.setRequestedByType("HUMAN");
    task.setRequestedByName("Operador");
    task.setTitle("Avaliar orçamento");
    task.setDescription("Confirmar o teto financeiro.");
    task.setPriority("HIGH");
    task.setStatus("BLOCKED");
    task.setTaskKind("WORK");
    task.setCreatedAt(Instant.parse("2026-08-12T10:00:00Z"));
    task.setUpdatedAt(task.getCreatedAt());
    when(repository.findByStatusInOrderByUpdatedAtDescIdDesc(
            List.of("IN_PROGRESS", "BLOCKED", "PENDING")))
        .thenReturn(List.of(task));

    AgentTaskFailureAuditResponse audit =
        service(repository, mock(AgentRepository.class), Clock.systemUTC())
            .activeTasks()
            .getFirst()
            .failureAudit();

    assertThat(audit.readiness()).isEqualTo("PARTIAL");
    assertThat(audit.missingEvidence())
        .containsExactly(
            "referência da entidade",
            "processo e atividade",
            "limite de autoridade",
            "causa da falha ou bloqueio",
            "orientação acionável com link",
            "evidências acessadas");
  }

  /** Não declara prontidão quando a evidência histórica não pode ser interpretada como JSON. */
  @Test
  void rejectsInvalidJsonAsCompleteFailureEvidence() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    Agent assignee = agent(3L, "financial-agent", "Plutus");
    assignee.setAuthorityPolicy("Somente leitura e parecer financeiro.");
    AgentTask task =
        processTask(94L, assignee, process("PUBLISHED", "Plutus"), "budget", "BLOCKED");
    task.setExecutionError("Catálogo indisponível.");
    task.setEvidenceJson("não é json");
    when(repository.findByStatusInOrderByUpdatedAtDescIdDesc(
            List.of("IN_PROGRESS", "BLOCKED", "PENDING")))
        .thenReturn(List.of(task));

    AgentTaskFailureAuditResponse audit =
        service(repository, mock(AgentRepository.class), Clock.systemUTC())
            .activeTasks()
            .getFirst()
            .failureAudit();

    assertThat(audit.readiness()).isEqualTo("PARTIAL");
    assertThat(audit.missingEvidence())
        .containsExactly("orientação acionável com link", "evidências em JSON válido");
  }

  /** Impede concluir uma tarefa que ainda não foi iniciada. */
  @Test
  void rejectsInvalidStatusJump() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task = new AgentTask();
    task.setId(5L);
    task.setStatus("PENDING");
    when(repository.findById(5L)).thenReturn(Optional.of(task));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    assertThatThrownBy(
            () -> service.updateStatus(5L, new UpdateAgentTaskStatusRequest("COMPLETED")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Transição de status inválida");
  }

  /** Impede que o status operacional substitua a decisão formal de um gate. */
  @Test
  void requiresGateDecisionFromAssignedAgent() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    Agent plutus = agent(4L, "financial-agent", "Plutus");
    AgentTask task = new AgentTask();
    task.setId(51L);
    task.setAssignedAgent(plutus);
    task.setTaskKind("GATE_DECISION");
    task.setGateCode("VIDEO_BUDGET_APPROVAL");
    task.setGateStatus("PENDING");
    task.setStatus("IN_PROGRESS");
    task.setCreatedAt(Instant.parse("2026-08-11T15:00:00Z"));
    task.setUpdatedAt(task.getCreatedAt());
    when(repository.findById(51L)).thenReturn(Optional.of(task));
    when(repository.save(task)).thenReturn(task);
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    assertThatThrownBy(
            () -> service.updateStatus(51L, new UpdateAgentTaskStatusRequest("COMPLETED")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("contrato de decisão");
    assertThatThrownBy(
            () ->
                service.decideGate(
                    51L, new DecideAgentGateRequest("videomaker", "APPROVED", "Dentro do teto.")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Somente o agente responsável");

    AgentTaskResponse approved =
        service.decideGate(
            51L, new DecideAgentGateRequest("financial-agent", "APPROVED", "Dentro do teto."));
    assertThat(approved.gateStatus()).isEqualTo("APPROVED");
    assertThat(approved.status()).isEqualTo("COMPLETED");
    assertThat(approved.gateDecisionReason()).isEqualTo("Dentro do teto.");
    assertThatThrownBy(
            () -> service.updateStatus(51L, new UpdateAgentTaskStatusRequest("IN_PROGRESS")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não pode ser reaberto");
  }

  /** Reserva somente a primeira atividade do processo e registra o recebimento real. */
  @Test
  void claimsFirstEligibleProcessTask() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent dedalo = agent(7L, "landing-generator", "Dédalo");
    BusinessProcessDefinition process = process("PUBLISHED", "Dédalo");
    AgentTask task = processTask(30L, dedalo, process, "html", "PENDING");
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(dedalo));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "landing-generator", "WORK", "PENDING"))
        .thenReturn(List.of(task));
    when(repository.save(task)).thenReturn(task);
    Instant received = Instant.parse("2026-08-15T12:00:00Z");
    AgentTaskService service =
        new AgentTaskService(
            repository,
            agents,
            mock(BusinessProcessDefinitionRepository.class),
            new ObjectMapper(),
            Clock.fixed(received, ZoneOffset.UTC));

    AgentTaskPendingResponse pending =
        service.claimEligibleProcessTask("landing-generator").orElseThrow();

    assertThat(pending.taskId()).isEqualTo(30L);
    assertThat(pending.receivedAt()).isEqualTo(received);
    assertThat(task.getStatus()).isEqualTo("IN_PROGRESS");
  }

  /** Entrega ao executor a identidade comercial resolvida pelo backend para a tarefa reservada. */
  @Test
  void includesTypedCommercialTargetInPendingContract() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    BusinessProcessDefinition process = process("PUBLISHED", "Psique");
    AgentTask task = processTask(254L, psique, process, "html", "PENDING");
    task.setSourceReference("experiment:89");
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "PENDING"))
        .thenReturn(List.of(task));
    when(repository.save(task)).thenReturn(task);
    AgentTaskTargetResponse expectedTarget =
        new AgentTaskTargetResponse(
            "experiment:89",
            89L,
            9L,
            "kit-whatsapp-pronto",
            "Kit WhatsApp Pronto",
            "Rigel",
            "kit-whatsapp-pronto-pde-v2",
            null,
            "https://checkout.example/rigel",
            new BigDecimal("349.00"));
    AgentTaskService service =
        new AgentTaskService(
            repository,
            null,
            agents,
            mock(BusinessProcessDefinitionRepository.class),
            null,
            null,
            new ObjectMapper(),
            null,
            Clock.systemUTC(),
            MarketStrategicContextProvider.empty(),
            sourceReference -> Optional.of(expectedTarget));

    AgentTaskPendingResponse pending =
        service.claimEligibleProcessTask("customer-agent").orElseThrow();

    assertThat(pending.taskTarget()).isEqualTo(expectedTarget);
  }

  /** Reserva idempotentemente a tarefa exata correlacionada por um ciclo técnico do backend. */
  @Test
  void claimsExactLinkedProcessTask() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent argos = agent(8L, "market-radar", "Argos");
    AgentTask task =
        processTask(37L, argos, process("PUBLISHED", "Argos e Dédalo"), "inspiration", "PENDING");
    when(agents.findByAgentKey("market-radar")).thenReturn(Optional.of(argos));
    when(repository.findById(37L)).thenReturn(Optional.of(task));
    when(repository.save(task)).thenReturn(task);
    Instant received = Instant.parse("2026-08-28T01:00:00Z");
    AgentTaskService service = service(repository, agents, Clock.fixed(received, ZoneOffset.UTC));

    AgentTaskPendingResponse first = service.claimLinkedProcessTask("market-radar", 37L);
    AgentTaskPendingResponse repeated = service.claimLinkedProcessTask("market-radar", 37L);

    assertThat(first.taskId()).isEqualTo(37L);
    assertThat(repeated.taskId()).isEqualTo(37L);
    assertThat(task.getStatus()).isEqualTo("IN_PROGRESS");
    assertThat(task.getReceivedAt()).isEqualTo(received);
    verify(repository).save(task);
  }

  /** Registra o primeiro recebimento comprovável ao retomar uma tarefa retroativa em andamento. */
  @Test
  void restoresReceiptForBackfilledInProgressTask() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent argos = agent(8L, "market-radar", "Argos");
    AgentTask task =
        processTask(
            37L, argos, process("PUBLISHED", "Argos e Dédalo"), "inspiration", "IN_PROGRESS");
    task.setReceivedAt(null);
    when(agents.findByAgentKey("market-radar")).thenReturn(Optional.of(argos));
    when(repository.findById(37L)).thenReturn(Optional.of(task));
    when(repository.save(task)).thenReturn(task);
    Instant resumed = Instant.parse("2026-08-28T01:10:00Z");

    AgentTaskPendingResponse pending =
        service(repository, agents, Clock.fixed(resumed, ZoneOffset.UTC))
            .claimLinkedProcessTask("market-radar", 37L);

    assertThat(pending.receivedAt()).isEqualTo(resumed);
    assertThat(task.getReceivedAt()).isEqualTo(resumed);
    verify(repository).save(task);
  }

  /** Registra fallback determinístico como custo não aplicável sem inventar uso de modelo. */
  @Test
  void recordsDeterministicExecutionAuditWithoutModelTokens() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    Agent argos = agent(8L, "market-radar", "Argos");
    AgentTask task =
        processTask(
            37L, argos, process("PUBLISHED", "Argos e Dédalo"), "inspiration", "IN_PROGRESS");
    when(repository.findById(37L)).thenReturn(Optional.of(task));
    when(repository.save(task)).thenReturn(task);
    Instant audited = Instant.parse("2026-08-28T01:05:00Z");
    AgentTaskService service =
        service(repository, mock(AgentRepository.class), Clock.fixed(audited, ZoneOffset.UTC));

    service.recordClaimedProcessTaskExecutionAudit(
        "market-radar",
        37L,
        "deterministic-fallback-v1",
        null,
        "{\"source\":\"persisted-context\"}",
        null,
        "{\"source\":\"persisted-context\"}",
        null,
        null,
        null,
        false);

    assertThat(task.getInputTokens()).isZero();
    assertThat(task.getCachedInputTokens()).isZero();
    assertThat(task.getOutputTokens()).isZero();
    assertThat(task.getEstimatedCostUsd()).isEqualByComparingTo("0.00000000");
    assertThat(task.getCostEstimationStatus()).isEqualTo("NOT_APPLICABLE");
    assertThat(task.getExecutionModelCode()).isEqualTo("deterministic-fallback-v1");
    assertThat(task.getModelUsageUpdatedAt()).isEqualTo(audited);
  }

  /** Rejeita auditoria parcial para não persistir consumo impossível de interpretar. */
  @Test
  void rejectsIncompleteLinkedExecutionTokenAudit() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            37L,
            agent(8L, "market-radar", "Argos"),
            process("PUBLISHED", "Argos e Dédalo"),
            "inspiration",
            "IN_PROGRESS");
    when(repository.findById(37L)).thenReturn(Optional.of(task));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.recordClaimedProcessTaskExecutionAudit(
                    "market-radar",
                    37L,
                    "gpt-5.6-sol",
                    null,
                    "Núcleo de Argos.\n\nPrompt real.",
                    "Núcleo de Argos.",
                    "Prompt real.",
                    100L,
                    null,
                    10L,
                    true))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("todos os contadores");
    verify(repository, never()).save(any());
  }

  /** Não reoferece uma lease ativa a outro polling sem identidade da execução original. */
  @Test
  void doesNotResumeClaimedProcessTaskFromPendingQueue() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent dedalo = agent(7L, "landing-generator", "Dédalo");
    AgentTask claimed =
        processTask(30L, dedalo, process("PUBLISHED", "Dédalo"), "html", "IN_PROGRESS");
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(dedalo));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "landing-generator", "WORK", "IN_PROGRESS"))
        .thenReturn(List.of(claimed));

    assertThat(
            service(repository, agents, Clock.systemUTC())
                .claimEligibleProcessTask("landing-generator"))
        .isEmpty();
    verify(repository, never()).save(any());
  }

  /** Retoma uma vez o trabalho bloqueado quando o callback falhou por indisponibilidade HTTP. */
  @Test
  void retriesTransientCallbackFailureOnlyOnce() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent dedalo = agent(7L, "landing-generator", "Dédalo");
    AgentTask blocked = processTask(30L, dedalo, process("PUBLISHED", "Dédalo"), "html", "BLOCKED");
    blocked.setExecutionError("500 : Internal Server Error");
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(dedalo));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "landing-generator", "WORK", "BLOCKED"))
        .thenReturn(List.of(blocked));
    when(repository.save(blocked)).thenReturn(blocked);

    AgentTaskPendingResponse recovered =
        service(repository, agents, Clock.systemUTC())
            .claimEligibleProcessTask("landing-generator")
            .orElseThrow();

    assertThat(recovered.taskId()).isEqualTo(30L);
    assertThat(blocked.getStatus()).isEqualTo("IN_PROGRESS");
    assertThat(blocked.getExecutionError()).startsWith("AUTO_RETRY_ONCE|");
  }

  /** Retoma uma vez a candidata bloqueada pelo contrato de checkout corrigido no backend. */
  @Test
  void retriesCorrectedCheckoutValidationOnlyOnce() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent dedalo = agent(7L, "landing-generator", "Dédalo");
    AgentTask blocked = processTask(30L, dedalo, process("PUBLISHED", "Dédalo"), "html", "BLOCKED");
    blocked.setExecutionError(
        "Dédalo produziu a candidata, mas o backend não conseguiu aplicá-la: "
            + "HTML integral alterou o destino protegido do checkout");
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(dedalo));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "landing-generator", "WORK", "BLOCKED"))
        .thenReturn(List.of(blocked));
    when(repository.save(blocked)).thenReturn(blocked);

    AgentTaskPendingResponse recovered =
        service(repository, agents, Clock.systemUTC())
            .claimEligibleProcessTask("landing-generator", "landing-page-generation", "html")
            .orElseThrow();

    assertThat(recovered.taskId()).isEqualTo(30L);
    assertThat(blocked.getStatus()).isEqualTo("IN_PROGRESS");
    assertThat(blocked.getExecutionError()).startsWith("AUTO_RETRY_ONCE|");
  }

  /** Mantém a atividade seguinte bloqueada enquanto sua predecessora não foi entregue. */
  @Test
  void blocksNextProcessTaskUntilPredecessorCompletes() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    BusinessProcessDefinition process = process("PUBLISHED", "Psique");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"html\",\"type\":\"TASK\"},{\"id\":\"customer\",\"type\":\"TASK\"}],"
            + "\"edges\":[{\"source\":\"html\",\"target\":\"customer\"}]}");
    AgentTask customer = processTask(31L, psique, process, "customer", "PENDING");
    AgentTask html =
        processTask(30L, agent(7L, "landing-generator", "Dédalo"), process, "html", "IN_PROGRESS");
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "PENDING"))
        .thenReturn(List.of(customer));
    when(repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            9L, "commercial-plan:2@v4"))
        .thenReturn(List.of(html, customer));
    AgentTaskService service = service(repository, agents, Clock.systemUTC());

    assertThat(service.claimEligibleProcessTask("customer-agent")).isEmpty();

    html.setStatus("COMPLETED");
    assertThat(service.claimEligibleProcessTask("customer-agent")).isPresent();
  }

  /** Exige que todos os responsáveis da mesma atividade entreguem antes de liberar a sucessora. */
  @Test
  void blocksSuccessorUntilEveryCoOwnerCompletesSharedActivity() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent plutus = agent(3L, "financial-agent", "Plutus");
    BusinessProcessDefinition process = process("PUBLISHED", "Plutus");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"review\",\"type\":\"TASK\"},{\"id\":\"economics\",\"type\":\"TASK\"}],"
            + "\"flows\":[{\"from\":\"review\",\"to\":\"economics\"}]}");
    AgentTask psiqueReview =
        processTask(40L, agent(2L, "customer-agent", "Psique"), process, "review", "COMPLETED");
    AgentTask temisReview =
        processTask(41L, agent(6L, "meta-ad-approver", "Têmis"), process, "review", "PENDING");
    AgentTask economics = processTask(42L, plutus, process, "economics", "PENDING");
    when(agents.findByAgentKey("financial-agent")).thenReturn(Optional.of(plutus));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "financial-agent", "WORK", "PENDING"))
        .thenReturn(List.of(economics));
    when(repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            9L, "commercial-plan:2@v4"))
        .thenReturn(List.of(psiqueReview, temisReview, economics));
    AgentTaskService service = service(repository, agents, Clock.systemUTC());

    assertThat(service.claimEligibleProcessTask("financial-agent")).isEmpty();

    temisReview.setStatus("COMPLETED");
    assertThat(service.claimEligibleProcessTask("financial-agent")).isPresent();
  }

  /**
   * Considera a correção mais recente sem deixar uma tentativa antiga bloquear o fluxo para sempre.
   */
  @Test
  void allowsSuccessorWhenLatestAttemptFromEveryCoOwnerCompletes() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    Agent temis = agent(6L, "meta-ad-approver", "Têmis");
    Agent plutus = agent(3L, "financial-agent", "Plutus");
    BusinessProcessDefinition process = process("PUBLISHED", "Plutus");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"review\",\"type\":\"TASK\"},{\"id\":\"economics\",\"type\":\"TASK\"}],"
            + "\"flows\":[{\"from\":\"review\",\"to\":\"economics\"}]}");
    AgentTask failedReview = processTask(40L, psique, process, "review", "BLOCKED");
    AgentTask correctedReview = processTask(41L, psique, process, "review", "COMPLETED");
    AgentTask coOwnerReview = processTask(42L, temis, process, "review", "COMPLETED");
    AgentTask economics = processTask(43L, plutus, process, "economics", "PENDING");
    when(agents.findByAgentKey("financial-agent")).thenReturn(Optional.of(plutus));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "financial-agent", "WORK", "PENDING"))
        .thenReturn(List.of(economics));
    when(repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            9L, "commercial-plan:2@v4"))
        .thenReturn(List.of(failedReview, correctedReview, coOwnerReview, economics));

    assertThat(
            service(repository, agents, Clock.systemUTC())
                .claimEligibleProcessTask("financial-agent"))
        .isPresent();
  }

  /** Respeita o formato flows/from/to usado pelas definições publicadas no editor BPM. */
  @Test
  void blocksSuccessorUsingPublishedFlowSchema() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    BusinessProcessDefinition process = process("PUBLISHED", "Psique");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"html\",\"type\":\"TASK\"},{\"id\":\"gate\",\"type\":\"GATEWAY\"},{\"id\":\"customer\",\"type\":\"TASK\"}],"
            + "\"flows\":[{\"from\":\"html\",\"to\":\"gate\"},{\"from\":\"gate\",\"to\":\"customer\"}]}");
    AgentTask customer = processTask(31L, psique, process, "customer", "PENDING");
    AgentTask html =
        processTask(30L, agent(7L, "landing-generator", "Dédalo"), process, "html", "IN_PROGRESS");
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "IN_PROGRESS"))
        .thenReturn(List.of());
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "PENDING"))
        .thenReturn(List.of(customer));
    when(repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            9L, "commercial-plan:2@v4"))
        .thenReturn(List.of(html, customer));

    assertThat(
            service(repository, agents, Clock.systemUTC())
                .claimEligibleProcessTask("customer-agent"))
        .isEmpty();
  }

  /** Ignora atividades automáticas sem tarefa própria ao ordenar as tarefas dos agentes. */
  @Test
  void traversesAutomaticActivityBetweenAgentTasks() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    BusinessProcessDefinition process = process("PUBLISHED", "Psique");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"html\",\"type\":\"TASK\"},{\"id\":\"technical\",\"type\":\"TASK\"},{\"id\":\"gate\",\"type\":\"GATEWAY\"},{\"id\":\"customer\",\"type\":\"TASK\"}],"
            + "\"flows\":[{\"from\":\"html\",\"to\":\"technical\"},{\"from\":\"technical\",\"to\":\"gate\"},{\"from\":\"gate\",\"to\":\"customer\"}]}");
    AgentTask html =
        processTask(30L, agent(7L, "landing-generator", "Dédalo"), process, "html", "COMPLETED");
    AgentTask customer = processTask(31L, psique, process, "customer", "PENDING");
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "IN_PROGRESS"))
        .thenReturn(List.of());
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "PENDING"))
        .thenReturn(List.of(customer));
    when(repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            9L, "commercial-plan:2@v4"))
        .thenReturn(List.of(html, customer));

    assertThat(
            service(repository, agents, Clock.systemUTC())
                .claimEligibleProcessTask("customer-agent"))
        .isPresent();
  }

  /** Ignora sucessoras alcançáveis somente pelo laço de retrabalho ao liberar Psique. */
  @Test
  void ignoresDownstreamTasksInsideReworkLoop() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    BusinessProcessDefinition process = process("PUBLISHED", "Psique");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"start\",\"type\":\"START\"},{\"id\":\"html\",\"type\":\"TASK\"},{\"id\":\"technical\",\"type\":\"TASK\"},{\"id\":\"technicalGate\",\"type\":\"GATEWAY\"},{\"id\":\"customer\",\"type\":\"TASK\"},{\"id\":\"customerGate\",\"type\":\"GATEWAY\"},{\"id\":\"commercial\",\"type\":\"TASK\"},{\"id\":\"commercialGate\",\"type\":\"GATEWAY\"},{\"id\":\"adjust\",\"type\":\"TASK\"}],"
            + "\"flows\":[{\"from\":\"start\",\"to\":\"html\"},{\"from\":\"html\",\"to\":\"technical\"},{\"from\":\"technical\",\"to\":\"technicalGate\"},{\"from\":\"technicalGate\",\"to\":\"customer\"},{\"from\":\"customer\",\"to\":\"customerGate\"},{\"from\":\"customerGate\",\"to\":\"commercial\"},{\"from\":\"commercial\",\"to\":\"commercialGate\"},{\"from\":\"commercialGate\",\"to\":\"adjust\"},{\"from\":\"adjust\",\"to\":\"technical\"}]}");
    AgentTask html =
        processTask(30L, agent(7L, "landing-generator", "Dédalo"), process, "html", "COMPLETED");
    AgentTask customer = processTask(31L, psique, process, "customer", "PENDING");
    AgentTask commercial =
        processTask(32L, agent(6L, "meta-ad-approver", "Têmis"), process, "commercial", "PENDING");
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "IN_PROGRESS"))
        .thenReturn(List.of());
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "PENDING"))
        .thenReturn(List.of(customer));
    when(repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            9L, "commercial-plan:2@v4"))
        .thenReturn(List.of(html, customer, commercial));

    assertThat(
            service(repository, agents, Clock.systemUTC())
                .claimEligibleProcessTask("customer-agent"))
        .isPresent();
  }

  /** Expõe atividade liberada, bloqueio por predecessora e tarefa legada substituída. */
  @Test
  void buildsProcessInstanceOperationalView() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    BusinessProcessDefinition process = process("PUBLISHED", "Dédalo");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"html\",\"type\":\"TASK\"},{\"id\":\"customer\",\"type\":\"TASK\"}],"
            + "\"edges\":[{\"source\":\"html\",\"target\":\"customer\"}]}");
    AgentTask html =
        processTask(30L, agent(7L, "landing-generator", "Dédalo"), process, "html", "PENDING");
    AgentTask customer =
        processTask(31L, agent(2L, "customer-agent", "Psique"), process, "customer", "PENDING");
    AgentTask legacy =
        processTask(27L, agent(7L, "landing-generator", "Dédalo"), process, "legacy", "PENDING");
    legacy.setProcessDefinition(null);
    when(repository.findBySourceReferenceOrderByCreatedAtAscIdAsc("commercial-plan:2@v4"))
        .thenReturn(List.of(legacy, html, customer));
    when(repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            9L, "commercial-plan:2@v4"))
        .thenReturn(List.of(html, customer));

    ProcessInstanceResponse instance =
        service(repository, mock(AgentRepository.class), Clock.systemUTC())
            .processInstances("commercial-plan:2@v4")
            .get(0);

    assertThat(instance.tasks())
        .extracting(ProcessInstanceTaskResponse::operationalState)
        .containsExactly("RELEASED", "WAITING_PREDECESSOR");
    assertThat(instance.supersededLegacyTasks())
        .extracting(ProcessInstanceTaskResponse::taskId)
        .containsExactly(27L);
  }

  /** Persiste saída e evidência antes de concluir a atividade reservada. */
  @Test
  void completesClaimedTaskWithAuditableResult() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            30L,
            agent(7L, "landing-generator", "Dédalo"),
            process("PUBLISHED", "Dédalo"),
            "html",
            "IN_PROGRESS");
    when(repository.findById(30L)).thenReturn(Optional.of(task));
    when(repository.save(task)).thenReturn(task);
    Instant delivered = Instant.parse("2026-08-15T13:00:00Z");
    AgentTaskService service =
        service(repository, mock(AgentRepository.class), Clock.fixed(delivered, ZoneOffset.UTC));

    service.completeClaimedProcessTask(
        "landing-generator",
        30L,
        new CompleteAgentTaskRequest(
            "{\"decision\":\"READY\"}", "{\"htmlVersion\":2}", null, modelExecutionAudit()));

    assertThat(task.getStatus()).isEqualTo("COMPLETED");
    assertThat(task.getDeliveredAt()).isEqualTo(delivered);
    assertThat(task.getResultJson()).contains("READY");
    assertThat(task.getEvidenceJson()).contains("htmlVersion");
  }

  /** Preserva a chamada final para tornar a execução reproduzível pela auditoria BPM. */
  @Test
  void preservesExecutionModelReasoningAndPrompt() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            30L,
            agent(7L, "landing-generator", "Dédalo"),
            process("PUBLISHED", "Dédalo"),
            "html",
            "IN_PROGRESS");
    when(repository.findById(30L)).thenReturn(Optional.of(task));
    when(repository.save(task)).thenReturn(task);
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    service.completeClaimedProcessTask(
        "landing-generator",
        30L,
        new CompleteAgentTaskRequest(
            "{\"decision\":\"READY\"}",
            "{\"htmlVersion\":2}",
            null,
            new AgentTaskExecutionAuditRequest(
                "MODEL",
                "gpt-5.6-sol",
                "high",
                "Núcleo de Dédalo.\n\nPrompt final enviado ao modelo.",
                "Núcleo de Dédalo.",
                "Prompt final enviado ao modelo.",
                List.of())));

    assertThat(task.getExecutionModelCode()).isEqualTo("gpt-5.6-sol");
    assertThat(task.getExecutionReasoningEffort()).isEqualTo("high");
    assertThat(task.getExecutionPrompt())
        .isEqualTo("Núcleo de Dédalo.\n\nPrompt final enviado ao modelo.");
    assertThat(task.getExecutionAgentPrompt()).isEqualTo("Núcleo de Dédalo.");
    assertThat(task.getExecutionActivityPrompt()).isEqualTo("Prompt final enviado ao modelo.");
  }

  /**
   * Rejeita chamada de modelo sem as duas partes explícitas para impedir divisão inferida na tela.
   */
  @Test
  void rejectsModelCompletionWithoutExplicitPromptParts() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            30L,
            agent(7L, "landing-generator", "Dédalo"),
            process("PUBLISHED", "Dédalo"),
            "html",
            "IN_PROGRESS");
    when(repository.findById(30L)).thenReturn(Optional.of(task));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.completeClaimedProcessTask(
                    "landing-generator",
                    30L,
                    new CompleteAgentTaskRequest(
                        "{}",
                        "{}",
                        null,
                        new AgentTaskExecutionAuditRequest(
                            "MODEL",
                            "gpt-5.6-sol",
                            "high",
                            "Núcleo de Dédalo.\n\nConstrua a landing.",
                            null,
                            "Construa a landing.",
                            List.of()))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("parte do agente");
    verify(repository, never()).save(any());
  }

  /** Rejeita partes invertidas para comprovar que o prompt integral corresponde ao enviado. */
  @Test
  void rejectsPromptPartsOutsideDeclaredOrder() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            30L,
            agent(7L, "landing-generator", "Dédalo"),
            process("PUBLISHED", "Dédalo"),
            "html",
            "IN_PROGRESS");
    when(repository.findById(30L)).thenReturn(Optional.of(task));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.completeClaimedProcessTask(
                    "landing-generator",
                    30L,
                    new CompleteAgentTaskRequest(
                        "{}",
                        "{}",
                        null,
                        new AgentTaskExecutionAuditRequest(
                            "MODEL",
                            "gpt-5.6-sol",
                            "high",
                            "Construa a landing.\n\nNúcleo de Dédalo.",
                            "Núcleo de Dédalo.",
                            "Construa a landing.",
                            List.of()))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("nesta ordem");
    verify(repository, never()).save(any());
  }

  /** Aceita duas interações auditadas quando cada fase preserva agente antes da atividade. */
  @Test
  void acceptsPromptPartsAcrossOrderedModelPhases() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            30L,
            agent(8L, "market-radar", "Argos"),
            process("PUBLISHED", "Argos"),
            "marketEvidence",
            "IN_PROGRESS");
    when(repository.findById(30L)).thenReturn(Optional.of(task));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());
    String fullPrompt =
        "--- PLANEJAMENTO ---\nNúcleo do plano.\n\nPlaneje a pesquisa.\n\n"
            + "--- SÍNTESE FACTUAL ---\nNúcleo da síntese.\n\nSintetize as evidências.";
    String agentPrompt =
        "--- PLANEJAMENTO ---\nNúcleo do plano.\n\n"
            + "--- SÍNTESE FACTUAL ---\nNúcleo da síntese.";
    String activityPrompt =
        "--- PLANEJAMENTO ---\nPlaneje a pesquisa.\n\n"
            + "--- SÍNTESE FACTUAL ---\nSintetize as evidências.";

    service.completeClaimedProcessTask(
        "market-radar",
        30L,
        new CompleteAgentTaskRequest(
            "{}",
            "{}",
            null,
            new AgentTaskExecutionAuditRequest(
                "MODEL",
                "gpt-5.6-sol",
                "high",
                fullPrompt,
                agentPrompt,
                activityPrompt,
                List.of())));

    assertThat(task.getExecutionPrompt()).isEqualTo(fullPrompt);
    assertThat(task.getExecutionAgentPrompt()).isEqualTo(agentPrompt);
    assertThat(task.getExecutionActivityPrompt()).isEqualTo(activityPrompt);
    assertThat(task.getStatus()).isEqualTo("COMPLETED");
  }

  /** Rejeita dossiê multifase quando uma atividade antecede o núcleo de sua própria interação. */
  @Test
  void rejectsPromptPartsInsideAnInvertedModelPhase() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            30L,
            agent(8L, "market-radar", "Argos"),
            process("PUBLISHED", "Argos"),
            "marketEvidence",
            "IN_PROGRESS");
    when(repository.findById(30L)).thenReturn(Optional.of(task));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.completeClaimedProcessTask(
                    "market-radar",
                    30L,
                    new CompleteAgentTaskRequest(
                        "{}",
                        "{}",
                        null,
                        new AgentTaskExecutionAuditRequest(
                            "MODEL",
                            "gpt-5.6-sol",
                            "high",
                            "--- PLANEJAMENTO ---\nPlaneje.\n\nNúcleo.",
                            "--- PLANEJAMENTO ---\nNúcleo.",
                            "--- PLANEJAMENTO ---\nPlaneje.",
                            List.of()))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("nesta ordem");
    verify(repository, never()).save(any());
  }

  /** Não declara custo inaplicável quando o modelo foi chamado sem telemetria de tokens. */
  @Test
  void keepsModelCostNotReportedWhenUsageIsUnavailable() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            30L,
            agent(7L, "landing-generator", "Dédalo"),
            process("PUBLISHED", "Dédalo"),
            "html",
            "IN_PROGRESS");
    when(repository.findById(30L)).thenReturn(Optional.of(task));
    when(repository.save(task)).thenReturn(task);
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    service.completeClaimedProcessTask(
        "landing-generator",
        30L,
        new CompleteAgentTaskRequest(
            "{\"decision\":\"READY\"}", "{\"htmlVersion\":2}", List.of(), modelExecutionAudit()));

    assertThat(task.getExecutionMode()).isEqualTo("MODEL");
    assertThat(task.getInputTokens()).isNull();
    assertThat(task.getEstimatedCostUsd()).isNull();
    assertThat(task.getCostEstimationStatus()).isEqualTo("NOT_REPORTED");
  }

  /** Impede concluir uma chamada de modelo cuja configuração e entrada não foram preservadas. */
  @Test
  void rejectsModelCompletionWithoutIntegralExecutionAudit() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            30L,
            agent(7L, "landing-generator", "Dédalo"),
            process("PUBLISHED", "Dédalo"),
            "html",
            "IN_PROGRESS");
    when(repository.findById(30L)).thenReturn(Optional.of(task));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.completeClaimedProcessTask(
                    "landing-generator",
                    30L,
                    new CompleteAgentTaskRequest("{\"decision\":\"READY\"}", "{}")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("modo de execução auditável");
    verify(repository, never()).save(any());
  }

  /** Persiste tokens e acumula o custo calculado pelo backend em tentativas da mesma tarefa. */
  @Test
  void accumulatesModelUsageAndBackendEstimatedCost() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    OpenAiPricingService pricing = mock(OpenAiPricingService.class);
    AgentTask task =
        processTask(
            30L,
            agent(7L, "landing-generator", "Dédalo"),
            process("PUBLISHED", "Dédalo"),
            "html",
            "IN_PROGRESS");
    task.setInputTokens(100L);
    task.setCachedInputTokens(20L);
    task.setOutputTokens(30L);
    task.setEstimatedCostUsd(new BigDecimal("0.01000000"));
    task.setCostEstimationStatus("ESTIMATED");
    when(repository.findById(30L)).thenReturn(Optional.of(task));
    when(repository.save(task)).thenReturn(task);
    when(pricing.estimateTaskCost("gpt-test", "FLEX", 900L, 400L, 200L))
        .thenReturn(Optional.of(new BigDecimal("0.02000000")));
    Instant delivered = Instant.parse("2026-08-20T13:00:00Z");
    AgentTaskService service =
        new AgentTaskService(
            repository,
            mock(AgentRepository.class),
            mock(BusinessProcessDefinitionRepository.class),
            new ObjectMapper(),
            pricing,
            Clock.fixed(delivered, ZoneOffset.UTC));

    service.completeClaimedProcessTask(
        "landing-generator",
        30L,
        new CompleteAgentTaskRequest(
            "{\"decision\":\"READY\"}",
            "{\"htmlVersion\":2}",
            List.of(new AgentTaskModelUsageRequest("gpt-test", "FLEX", 900L, 400L, 200L)),
            modelExecutionAudit()));

    assertThat(task.getInputTokens()).isEqualTo(1_000L);
    assertThat(task.getCachedInputTokens()).isEqualTo(420L);
    assertThat(task.getOutputTokens()).isEqualTo(230L);
    assertThat(task.getEstimatedCostUsd()).isEqualByComparingTo("0.03000000");
    assertThat(task.getCostEstimationStatus()).isEqualTo("ESTIMATED");
    assertThat(task.getModelUsageUpdatedAt()).isEqualTo(delivered);
  }

  /** Preserva os tokens e sinaliza preço ausente quando o catálogo não conhece o modelo. */
  @Test
  void preservesTokensWhenPricingIsUnavailable() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    OpenAiPricingService pricing = mock(OpenAiPricingService.class);
    AgentTask task =
        processTask(
            31L,
            agent(2L, "customer-agent", "Psique"),
            process("PUBLISHED", "Psique"),
            "customer",
            "IN_PROGRESS");
    when(repository.findById(31L)).thenReturn(Optional.of(task));
    when(repository.save(task)).thenReturn(task);
    when(pricing.estimateTaskCost("unknown", "FLEX", 500L, 100L, 80L)).thenReturn(Optional.empty());
    AgentTaskService service =
        new AgentTaskService(
            repository,
            mock(AgentRepository.class),
            mock(BusinessProcessDefinitionRepository.class),
            new ObjectMapper(),
            pricing,
            Clock.systemUTC());

    service.failClaimedProcessTask(
        "customer-agent",
        31L,
        new FailAgentTaskRequest(
            "Ajuste necessário",
            "{\"decision\":\"ADJUST\"}",
            "{\"reviewer\":\"Psique\"}",
            List.of(new AgentTaskModelUsageRequest("unknown", "FLEX", 500L, 100L, 80L)),
            modelExecutionAudit(),
            null));

    assertThat(task.getInputTokens()).isEqualTo(500L);
    assertThat(task.getCachedInputTokens()).isEqualTo(100L);
    assertThat(task.getOutputTokens()).isEqualTo(80L);
    assertThat(task.getEstimatedCostUsd()).isNull();
    assertThat(task.getCostEstimationStatus()).isEqualTo("PRICING_UNAVAILABLE");
  }

  /** Rejeita cache maior que a entrada total antes de persistir um custo impossível. */
  @Test
  void rejectsCachedTokensGreaterThanInput() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            31L,
            agent(2L, "customer-agent", "Psique"),
            process("PUBLISHED", "Psique"),
            "customer",
            "IN_PROGRESS");
    when(repository.findById(31L)).thenReturn(Optional.of(task));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.completeClaimedProcessTask(
                    "customer-agent",
                    31L,
                    new CompleteAgentTaskRequest(
                        "{}",
                        "{}",
                        List.of(
                            new AgentTaskModelUsageRequest("gpt-test", "FLEX", 100L, 101L, 20L)))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Consumo de modelo inválido");
    verify(repository, never()).save(any());
  }

  /** Rejeita tier sem política de preço mesmo quando o serviço é chamado fora do controller. */
  @Test
  void rejectsUnsupportedServiceTier() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            31L,
            agent(2L, "customer-agent", "Psique"),
            process("PUBLISHED", "Psique"),
            "customer",
            "IN_PROGRESS");
    when(repository.findById(31L)).thenReturn(Optional.of(task));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.completeClaimedProcessTask(
                    "customer-agent",
                    31L,
                    new CompleteAgentTaskRequest(
                        "{}",
                        "{}",
                        List.of(
                            new AgentTaskModelUsageRequest(
                                "gpt-test", "UNMETERED", 100L, 0L, 20L)))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Consumo de modelo inválido");
    verify(repository, never()).save(any());
  }

  /** Preserva o parecer funcional quando o agente bloqueia a atividade por qualidade. */
  @Test
  void blocksClaimedTaskWithAuditableReview() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            31L,
            agent(2L, "customer-agent", "Psique"),
            process("PUBLISHED", "Psique"),
            "customer",
            "IN_PROGRESS");
    when(repository.findById(31L)).thenReturn(Optional.of(task));
    when(repository.save(task)).thenReturn(task);

    service(repository, mock(AgentRepository.class), Clock.systemUTC())
        .failClaimedProcessTask(
            "customer-agent",
            31L,
            new FailAgentTaskRequest(
                "Clareza insuficiente",
                "{\"decision\":\"ADJUST\"}",
                "{\"reviewer\":\"Psique\"}",
                null,
                new AgentTaskExecutionAuditRequest(
                    "MODEL",
                    "gpt-test",
                    "high",
                    "Núcleo de Psique.\n\nRevise a jornada humana.",
                    "Núcleo de Psique.",
                    "Revise a jornada humana.",
                    List.of(
                        new AgentTaskAccessedUrlRequest(
                            "https://rigel.example.com/jornada",
                            "Jornada Rigel",
                            "PLAYWRIGHT",
                            Instant.parse("2026-08-28T16:15:00Z")))),
                new AgentTaskBlockerGuidanceRequest(
                    "FUNCTIONAL_ADJUSTMENT",
                    "Corrigir a clareza da etapa de entrega e reiniciar a revisão.",
                    List.of(
                        new AgentTaskHelpLinkRequest("Abrir experimento", "/experiments/88")))));

    assertThat(task.getStatus()).isEqualTo("BLOCKED");
    assertThat(task.getResultJson()).contains("ADJUST");
    assertThat(task.getEvidenceJson()).contains("Psique");
    assertThat(task.getBlockerAction()).contains("Corrigir a clareza");
    assertThat(AgentTaskAuditView.accessedUrls(task))
        .singleElement()
        .satisfies(
            link -> {
              assertThat(link.url()).isEqualTo("https://rigel.example.com/jornada");
              assertThat(link.accessMethod()).isEqualTo("PLAYWRIGHT");
            });
    assertThat(AgentTaskAuditView.blockerGuidance(task).helpLinks())
        .extracting(AgentTaskAuditLinkResponse::url)
        .containsExactly("/experiments/88");
  }

  /** Rejeita URL com credencial para impedir que a auditoria exponha segredo na tela. */
  @Test
  void rejectsAccessedUrlWithSensitiveQueryParameter() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            31L,
            agent(2L, "customer-agent", "Psique"),
            process("PUBLISHED", "Psique"),
            "customer",
            "IN_PROGRESS");
    when(repository.findById(31L)).thenReturn(Optional.of(task));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.completeClaimedProcessTask(
                    "customer-agent",
                    31L,
                    new CompleteAgentTaskRequest(
                        "{}",
                        "{}",
                        null,
                        new AgentTaskExecutionAuditRequest(
                            "MODEL",
                            "gpt-test",
                            "high",
                            "Núcleo de Psique.\n\nRevise a jornada.",
                            "Núcleo de Psique.",
                            "Revise a jornada.",
                            List.of(
                                new AgentTaskAccessedUrlRequest(
                                    "https://example.com/review?api_key=secret",
                                    "Fonte",
                                    "HTTP",
                                    null))))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("URL de auditoria insegura");
    verify(repository, never()).save(any());
  }

  /** Rejeita segredo em link interno para não contornar a proteção dos links externos. */
  @Test
  void rejectsInternalHelpLinkWithSensitiveFragment() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentTask task =
        processTask(
            31L,
            agent(2L, "customer-agent", "Psique"),
            process("PUBLISHED", "Psique"),
            "customer",
            "IN_PROGRESS");
    when(repository.findById(31L)).thenReturn(Optional.of(task));
    AgentTaskService service = service(repository, mock(AgentRepository.class), Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.failClaimedProcessTask(
                    "customer-agent",
                    31L,
                    new FailAgentTaskRequest(
                        "Ajuste necessário",
                        "{\"decision\":\"ADJUST\"}",
                        "{}",
                        null,
                        modelExecutionAudit(),
                        new AgentTaskBlockerGuidanceRequest(
                            "FUNCTIONAL_ADJUSTMENT",
                            "Corrigir e reiniciar.",
                            List.of(
                                new AgentTaskHelpLinkRequest(
                                    "Abrir tarefa", "/agent-tasks#access_token=secret"))))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("URL de auditoria insegura");
    verify(repository, never()).save(any());
  }

  /** Entrega a Psique as evidências concluídas de Dédalo sem depender de logs técnicos. */
  @Test
  void exposesCompletedPredecessorEvidenceInPendingContext() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    BusinessProcessDefinition process = process("PUBLISHED", "Psique");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"html\",\"type\":\"TASK\"},{\"id\":\"customer\",\"type\":\"TASK\"}],"
            + "\"flows\":[{\"from\":\"html\",\"to\":\"customer\"}]}");
    AgentTask html =
        processTask(30L, agent(7L, "landing-generator", "Dédalo"), process, "html", "COMPLETED");
    html.setResultJson("{\"approvalRecommendation\":\"APPROVE_FOR_PUBLICATION\"}");
    html.setEvidenceJson("{\"desktopScreenshot\":\"draft://desktop.png\"}");
    AgentTask customer = processTask(31L, psique, process, "customer", "PENDING");
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "IN_PROGRESS"))
        .thenReturn(List.of());
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "PENDING"))
        .thenReturn(List.of(customer));
    when(repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            9L, "commercial-plan:2@v4"))
        .thenReturn(List.of(html, customer));

    AgentTaskPendingResponse pending =
        service(repository, agents, Clock.systemUTC())
            .claimEligibleProcessTask("customer-agent")
            .orElseThrow();

    assertThat(pending.processContextJson()).contains("APPROVE_FOR_PUBLICATION", "desktop.png");
  }

  /** Injeta a mesma estratégia de Atena nas tarefas de comunicação e operação do plano. */
  @Test
  void exposesMarketStrategicContractInPendingContext() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent hermes = agent(3L, "growth-operator", "Hermes");
    BusinessProcessDefinition process = process("PUBLISHED", "Hermes");
    process.setProcessCode("pde-communication-sales-journey");
    process.setDiagramJson("{\"nodes\":[{\"id\":\"contract\",\"type\":\"TASK\"}],\"flows\":[]}");
    AgentTask task = processTask(34L, hermes, process, "contract", "PENDING");
    when(agents.findByAgentKey("growth-operator")).thenReturn(Optional.of(hermes));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "growth-operator", "WORK", "IN_PROGRESS"))
        .thenReturn(List.of());
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "growth-operator", "WORK", "PENDING"))
        .thenReturn(List.of(task));
    when(repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            9L, "commercial-plan:2@v4"))
        .thenReturn(List.of(task));
    MarketStrategicContextProvider provider =
        sourceReference ->
            Optional.of(
                Map.of(
                    "availability",
                    "AVAILABLE",
                    "contractVersion",
                    "MARKET_STRATEGY_V2",
                    "contentHash",
                    "a".repeat(64)));
    AgentTaskService service =
        new AgentTaskService(
            repository,
            null,
            agents,
            mock(BusinessProcessDefinitionRepository.class),
            null,
            null,
            new ObjectMapper(),
            null,
            Clock.systemUTC(),
            provider);

    AgentTaskPendingResponse pending =
        service
            .claimEligibleProcessTask(
                "growth-operator", "pde-communication-sales-journey", "contract")
            .orElseThrow();

    assertThat(pending.processContextJson())
        .contains("marketStrategicContract", "MARKET_STRATEGY_V2", "contentHash");
  }

  /** Envia somente a correção mais recente do mesmo responsável e atividade ao próximo agente. */
  @Test
  void compactsSupersededCompletedAttemptsFromPendingContext() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent dedalo = agent(7L, "landing-generator", "Dédalo");
    Agent psique = agent(2L, "customer-agent", "Psique");
    BusinessProcessDefinition process = process("PUBLISHED", "Psique");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"html\",\"type\":\"TASK\"},{\"id\":\"customer\",\"type\":\"TASK\"}],"
            + "\"flows\":[{\"from\":\"html\",\"to\":\"customer\"}]}");
    AgentTask oldHtml = processTask(30L, dedalo, process, "html", "COMPLETED");
    oldHtml.setResultJson("{\"version\":\"superseded\"}");
    AgentTask currentHtml = processTask(32L, dedalo, process, "html", "COMPLETED");
    currentHtml.setResultJson("{\"version\":\"current\"}");
    AgentTask customer = processTask(33L, psique, process, "customer", "PENDING");
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "IN_PROGRESS"))
        .thenReturn(List.of());
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "PENDING"))
        .thenReturn(List.of(customer));
    when(repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            9L, "commercial-plan:2@v4"))
        .thenReturn(List.of(oldHtml, currentHtml, customer));

    AgentTaskPendingResponse pending =
        service(repository, agents, Clock.systemUTC())
            .claimEligibleProcessTask("customer-agent")
            .orElseThrow();

    assertThat(pending.processContextJson()).contains("current").doesNotContain("superseded");
  }

  /** Não deixa o consumidor de landing reservar uma tarefa de outro processo de Psique. */
  @Test
  void filtersPendingTaskByExecutorContract() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    AgentTask unrelated =
        processTask(40L, psique, process("PUBLISHED", "Psique"), "research", "PENDING");
    unrelated.getProcessDefinition().setProcessCode("product-research");
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "IN_PROGRESS"))
        .thenReturn(List.of());
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "customer-agent", "WORK", "PENDING"))
        .thenReturn(List.of(unrelated));

    assertThat(
            service(repository, agents, Clock.systemUTC())
                .claimEligibleProcessTask("customer-agent", "landing-page-generation", "customer"))
        .isEmpty();
    verify(repository, never()).save(any());
  }

  /** Impede o executor comum de reservar atividade que exige um container especializado. */
  @Test
  void routesSpecializedActivityOnlyToMatchingExecutionResource() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    BusinessProcessExecutionResourceRepository resources =
        mock(BusinessProcessExecutionResourceRepository.class);
    Agent temis = agent(6L, "meta-ad-approver", "Têmis");
    BusinessProcessDefinition process = process("PUBLISHED", "Têmis");
    process.setProcessCode("pde-construction-approval");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"deliverables\",\"type\":\"TASK\","
            + "\"label\":\"Produzir entregáveis premium\",\"owner\":\"Têmis\","
            + "\"executionResourceCode\":\"themis-image-studio\"}],\"flows\":[]}");
    AgentTask task = processTask(88L, temis, process, "deliverables", "PENDING");
    when(agents.findByAgentKey("meta-ad-approver")).thenReturn(Optional.of(temis));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "meta-ad-approver", "WORK", "IN_PROGRESS"))
        .thenReturn(List.of());
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "meta-ad-approver", "WORK", "PENDING"))
        .thenReturn(List.of(task));
    when(repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            9L, "commercial-plan:2@v4"))
        .thenReturn(List.of(task));
    when(repository.save(task)).thenReturn(task);
    when(resources.findByResourceCodeAndActiveTrue("themis-image-studio"))
        .thenReturn(Optional.of(studio()));
    AgentTaskService service =
        new AgentTaskService(
            repository,
            agents,
            mock(BusinessProcessDefinitionRepository.class),
            resources,
            new ObjectMapper(),
            null,
            Clock.systemUTC());

    assertThat(
            service.claimEligibleProcessTask(
                "meta-ad-approver", "pde-construction-approval", "deliverables"))
        .isEmpty();

    AgentTaskPendingResponse pending =
        service
            .claimEligibleProcessTask(
                "meta-ad-approver",
                "pde-construction-approval",
                "deliverables",
                "themis-image-studio")
            .orElseThrow();

    assertThat(pending.executionResource()).isNotNull();
    assertThat(pending.executionResource().resourceCode()).isEqualTo("themis-image-studio");
    assertThat(pending.executionResource().executorReference()).isEqualTo("themis-image-studio");
    assertThat(task.getStatus()).isEqualTo("IN_PROGRESS");
  }

  /** Bloqueia vínculo quando o recurso especializado pertence a outro agente. */
  @Test
  void rejectsActivityResourceAssignedToAnotherAgent() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    BusinessProcessExecutionResourceRepository resources =
        mock(BusinessProcessExecutionResourceRepository.class);
    Agent psique = agent(2L, "customer-agent", "Psique");
    BusinessProcessDefinition process = process("PUBLISHED", "Psique");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"html\",\"type\":\"TASK\",\"label\":\"Avaliar\","
            + "\"owner\":\"Psique\",\"executionResourceCode\":\"themis-image-studio\"}],"
            + "\"flows\":[]}");
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(processes.findById(9L)).thenReturn(Optional.of(process));
    when(resources.findByResourceCodeAndActiveTrue("themis-image-studio"))
        .thenReturn(Optional.of(studio()));
    AgentTaskService service =
        new AgentTaskService(
            repository, agents, processes, resources, new ObjectMapper(), null, Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.createByHuman(
                    new CreateAgentTaskRequest(
                        "customer-agent",
                        "Operador",
                        "Avaliar",
                        "Avaliar entrega.",
                        "HIGH",
                        "experiment:88",
                        9L,
                        "html",
                        false,
                        null)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("outro agente");
    verify(repository, never()).save(any());
  }

  /** Cria um agente mínimo para os cenários do serviço. */
  private Agent agent(Long id, String key, String nickname) {
    Agent value = new Agent();
    value.setId(id);
    value.setAgentKey(key);
    value.setNickname(nickname);
    value.setName(nickname);
    return value;
  }

  /** Cria a solicitação regular reutilizada nos cenários de tentativa e nova ocorrência. */
  private CreateAgentTaskRequest regularActivityRequest(String title) {
    return new CreateAgentTaskRequest(
        "landing-generator",
        "Operador",
        title,
        "Entregar HTML responsivo.",
        "HIGH",
        "experiment:88",
        9L,
        "html",
        false,
        null);
  }

  /** Cria a solicitação usada para validar um dos coautores técnicos da atividade. */
  private CreateAgentTaskRequest coauthorRequest(String agentKey) {
    return new CreateAgentTaskRequest(
        agentKey,
        "Operador",
        "Validar PDE",
        "Comprovar fatos, controle e valor.",
        "HIGH",
        "experiment:90",
        9L,
        "html",
        false,
        null);
  }

  /** Cria uma auditoria integral de modelo reutilizada nos callbacks de teste. */
  private AgentTaskExecutionAuditRequest modelExecutionAudit() {
    return new AgentTaskExecutionAuditRequest(
        "MODEL",
        "gpt-test",
        "high",
        "Núcleo do agente.\n\nAtividade da tarefa.",
        "Núcleo do agente.",
        "Atividade da tarefa.",
        List.of());
  }

  /** Monta o Estúdio de Têmis com instruções entregues ao executor. */
  private BusinessProcessExecutionResource studio() {
    BusinessProcessExecutionResource resource = new BusinessProcessExecutionResource();
    resource.setResourceCode("themis-image-studio");
    resource.setName("Estúdio de Imagens de Têmis");
    resource.setResourceType("CONTAINER");
    resource.setResponsibleAgentKey("meta-ad-approver");
    resource.setExecutorReference("themis-image-studio");
    resource.setUsageInstructions("Consumir o endpoint pending do backend.");
    resource.setActive(true);
    return resource;
  }

  /** Cria uma definição mínima com atividade atribuída para validar o vínculo. */
  private BusinessProcessDefinition process(String status, String owner) {
    BusinessProcessDefinition value = new BusinessProcessDefinition();
    value.setId(9L);
    value.setProcessCode("landing-page-generation");
    value.setVersionNumber(1);
    value.setStatus(status);
    value.setDiagramJson(
        "{\"nodes\":[{\"id\":\"html\",\"type\":\"TASK\",\"label\":\"Montar HTML\",\"owner\":\""
            + owner
            + "\"}],\"edges\":[]}");
    return value;
  }

  /** Cria uma tarefa vinculada a uma execução comercial para validar a sequência. */
  private AgentTask processTask(
      Long id, Agent agent, BusinessProcessDefinition process, String activityId, String status) {
    AgentTask value = new AgentTask();
    value.setId(id);
    value.setAssignedAgent(agent);
    value.setRequestedByType("HUMAN");
    value.setRequestedByName("Operador");
    value.setTitle("Executar atividade");
    value.setDescription("Entregar resultado auditável.");
    value.setPriority("HIGH");
    value.setStatus(status);
    value.setTaskKind("WORK");
    value.setProcessDefinition(process);
    value.setProcessActivityId(activityId);
    value.setProcessActivityName(activityId);
    value.setSourceReference("commercial-plan:2@v4");
    value.setCreatedAt(Instant.parse("2026-08-15T04:12:00Z"));
    value.setUpdatedAt(value.getCreatedAt());
    return value;
  }

  /** Monta o serviço com as dependências do vínculo BPM para testes isolados. */
  private AgentTaskService service(
      AgentTaskRepository repository, AgentRepository agents, Clock clock) {
    return new AgentTaskService(
        repository,
        agents,
        mock(BusinessProcessDefinitionRepository.class),
        new ObjectMapper(),
        clock);
  }
}
