package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
            + "\"}]}");
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
