package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: validar o ciclo assíncrono entre materialização e gate técnico. */
class AgentTaskDeferredCompletionTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Mantém a tarefa ativa até o gate e preserva separadamente as duas evidências. */
  @Test
  void shouldCompleteOnlyAfterDeferredGateApproval() throws Exception {
    AgentTask task = task(41L);
    AgentTaskService service = service(task);
    ReflectionTestUtils.setField(
        service,
        "completionHooks",
        List.of(
            new AgentTaskCompletionHook() {
              /** Intercepta a tarefa usada pelo cenário. */
              @Override
              public boolean supports(AgentTask candidate) {
                return candidate.getId().equals(41L);
              }

              /** Simula um efeito que depende de revisão técnica posterior. */
              @Override
              public CompletionDisposition apply(
                  AgentTask candidate, CompleteAgentTaskRequest request) {
                return CompletionDisposition.DEFERRED;
              }
            }));

    service.completeClaimedProcessTask(
        "communication-director",
        41L,
        new CompleteAgentTaskRequest(
            "{\"functionalOutput\":{\"landingHtml\":\"<html></html>\"}}",
            "{\"draftSha256\":\"abc\"}"));

    assertThat(task.getStatus()).isEqualTo("IN_PROGRESS");
    assertThat(task.getDeliveredAt()).isNull();
    assertThat(task.getResultJson()).contains("landingHtml");

    service.completeDeferredProcessTask(
        "communication-director", 41L, "{\"qualityReview\":\"APPROVED\"}");

    JsonNode evidence = objectMapper.readTree(task.getEvidenceJson());
    assertThat(task.getStatus()).isEqualTo("COMPLETED");
    assertThat(task.getDeliveredAt()).isNotNull();
    assertThat(evidence.path("materialization").path("draftSha256").asText()).isEqualTo("abc");
    assertThat(evidence.path("technicalGate").path("qualityReview").asText()).isEqualTo("APPROVED");
  }

  /** Bloqueia a tarefa reprovada sem apagar o artefato que chegou ao gate. */
  @Test
  void shouldBlockAfterDeferredGateRejection() throws Exception {
    AgentTask task = task(42L);
    task.setEvidenceJson("{\"draftSha256\":\"def\"}");
    AgentTaskService service = service(task);

    service.failDeferredProcessTask(
        "communication-director",
        42L,
        "Quality Review reprovou a candidata.",
        "{\"qualityReview\":\"REJECTED\"}");

    JsonNode evidence = objectMapper.readTree(task.getEvidenceJson());
    assertThat(task.getStatus()).isEqualTo("BLOCKED");
    assertThat(task.getExecutionError()).contains("reprovou");
    assertThat(task.getDeliveredAt()).isNull();
    assertThat(evidence.path("materialization").path("draftSha256").asText()).isEqualTo("def");
    assertThat(evidence.path("technicalGate").path("qualityReview").asText()).isEqualTo("REJECTED");
  }

  /** Monta o serviço determinístico que persiste a mesma entidade em memória. */
  private AgentTaskService service(AgentTask task) {
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    when(tasks.findById(task.getId())).thenReturn(Optional.of(task));
    when(tasks.save(any(AgentTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
    return new AgentTaskService(
        tasks,
        mock(AgentRepository.class),
        mock(BusinessProcessDefinitionRepository.class),
        objectMapper,
        Clock.fixed(Instant.parse("2026-08-28T12:00:00Z"), ZoneOffset.UTC));
  }

  /** Cria uma lease mínima pertencente exclusivamente a Íris. */
  private AgentTask task(Long id) {
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setAssignedAgent(Agent.builder().agentKey("communication-director").build());
    task.setStatus("IN_PROGRESS");
    task.setCreatedAt(Instant.parse("2026-08-28T11:00:00Z"));
    task.setUpdatedAt(task.getCreatedAt());
    return task;
  }
}
