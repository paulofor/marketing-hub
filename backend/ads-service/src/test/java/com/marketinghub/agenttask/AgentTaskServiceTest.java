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
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar autoria, segregação e ciclo de vida das tarefas dos agentes. */
class AgentTaskServiceTest {

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
    assertThat(response.status()).isEqualTo("PENDING");
    assertThat(response.createdAt()).isEqualTo(now);
    assertThat(response.receivedAt()).isNull();
    assertThat(response.deliveredAt()).isNull();
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

  /** Reoferece a lease ativa ao mesmo agente após reinício sem liberar sucessoras. */
  @Test
  void resumesClaimedProcessTaskBeforeClaimingAnother() {
    AgentTaskRepository repository = mock(AgentTaskRepository.class);
    AgentRepository agents = mock(AgentRepository.class);
    Agent dedalo = agent(7L, "landing-generator", "Dédalo");
    AgentTask claimed =
        processTask(30L, dedalo, process("PUBLISHED", "Dédalo"), "html", "IN_PROGRESS");
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(dedalo));
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            "landing-generator", "WORK", "IN_PROGRESS"))
        .thenReturn(List.of(claimed));

    AgentTaskPendingResponse resumed =
        service(repository, agents, Clock.systemUTC())
            .claimEligibleProcessTask("landing-generator")
            .orElseThrow();

    assertThat(resumed.taskId()).isEqualTo(30L);
    verify(repository, never()).save(any());
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
        new CompleteAgentTaskRequest("{\"decision\":\"READY\"}", "{\"htmlVersion\":2}"));

    assertThat(task.getStatus()).isEqualTo("COMPLETED");
    assertThat(task.getDeliveredAt()).isEqualTo(delivered);
    assertThat(task.getResultJson()).contains("READY");
    assertThat(task.getEvidenceJson()).contains("htmlVersion");
  }

  /** Cria um agente mínimo para os cenários do serviço. */
  private Agent agent(Long id, String key, String nickname) {
    Agent value = new Agent();
    value.setId(id);
    value.setAgentKey(key);
    value.setNickname(nickname);
    return value;
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
