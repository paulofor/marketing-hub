package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar limite, versões e auditoria do histórico de execuções BPM. */
class BusinessProcessActivityExecutionServiceTest {
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityExecutionService service =
      new BusinessProcessActivityExecutionService(processes, tasks, new ObjectMapper());

  /** Lista dez tarefas do processo canônico e preserva a versão real de cada execução. */
  @Test
  void returnsTenRecentExecutionsAcrossProcessVersions() {
    when(processes.findById(37L)).thenReturn(Optional.of(selectedProcess()));
    when(tasks.findRecentActivityExecutions(
            eq("pde-opportunity-discovery"), eq("evidence"), any(Pageable.class)))
        .thenReturn(LongStream.rangeClosed(1, 11).mapToObj(this::executionTask).toList());

    var result = service.recentExecutions(37L, "evidence");

    assertThat(result.processName()).isEqualTo("Descoberta e priorização da oportunidade PDE");
    assertThat(result.selectedProcessVersionNumber()).isEqualTo(4);
    assertThat(result.selectedProcessStatus()).isEqualTo("RETIRED");
    assertThat(result.activityName()).isEqualTo("Comprovar dor e demanda");
    assertThat(result.activityOwnerName()).isEqualTo("Argos");
    assertThat(result.executions()).hasSize(10);
    var execution = result.executions().getFirst();
    assertThat(execution.processDefinitionId()).isEqualTo(22L);
    assertThat(execution.processVersionNumber()).isEqualTo(1);
    assertThat(execution.comments()).contains("APPROVE");
    assertThat(execution.modelCode()).isEqualTo("gpt-5.4-mini-2026-03-17");
    assertThat(execution.reasoningEffort()).isEqualTo("high");
    assertThat(execution.promptSent()).isEqualTo("Comprove a dor com fontes independentes.");
    assertThat(execution.estimatedCostUsd()).isEqualByComparingTo("0.01347240");
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(tasks)
        .findRecentActivityExecutions(
            eq("pde-opportunity-discovery"), eq("evidence"), pageable.capture());
    assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
  }

  /** Usa a última atualização como término somente para uma tarefa terminal sem entrega. */
  @Test
  void exposesTerminalFailureTimeWithoutFinishingPendingTask() {
    when(processes.findById(37L)).thenReturn(Optional.of(selectedProcess()));
    AgentTask blocked = executionTask(1L);
    blocked.setStatus("BLOCKED");
    blocked.setDeliveredAt(null);
    AgentTask pending = executionTask(2L);
    pending.setStatus("PENDING");
    pending.setReceivedAt(null);
    pending.setDeliveredAt(null);
    when(tasks.findRecentActivityExecutions(anyString(), anyString(), any(Pageable.class)))
        .thenReturn(List.of(blocked, pending));

    var executions = service.recentExecutions(37L, "evidence").executions();

    assertThat(executions.get(0).finishedAt()).isEqualTo(blocked.getUpdatedAt());
    assertThat(executions.get(1).startedAt()).isNull();
    assertThat(executions.get(1).finishedAt()).isNull();
  }

  /** Bloqueia o uso de um gate como se ele possuísse tarefas executáveis. */
  @Test
  void rejectsNonTaskActivity() {
    when(processes.findById(37L)).thenReturn(Optional.of(selectedProcess()));

    assertThatThrownBy(() -> service.recentExecutions(37L, "gate"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Atividade não encontrada");
    verify(tasks, never()).findRecentActivityExecutions(anyString(), anyString(), any());
  }

  /** Monta a versão selecionada pelo usuário com atividade e gate distintos. */
  private BusinessProcessDefinition selectedProcess() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(37L);
    process.setProcessCode("pde-opportunity-discovery");
    process.setName("Descoberta e priorização da oportunidade PDE");
    process.setVersionNumber(4);
    process.setStatus("RETIRED");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"evidence\",\"type\":\"TASK\","
            + "\"label\":\"Comprovar dor e demanda\",\"owner\":\"Argos\"},"
            + "{\"id\":\"gate\",\"type\":\"GATEWAY\",\"label\":\"Aprovada?\"}]}");
    return process;
  }

  /** Monta uma tarefa da v1 para comprovar a consulta histórica iniciada na v4. */
  private AgentTask executionTask(long id) {
    BusinessProcessDefinition originalProcess = new BusinessProcessDefinition();
    originalProcess.setId(22L);
    originalProcess.setVersionNumber(1);
    Agent agent = new Agent();
    agent.setAgentKey("market-radar");
    agent.setNickname("Argos");
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setProcessDefinition(originalProcess);
    task.setAssignedAgent(agent);
    task.setTitle("Comprovar dor e demanda · rodada " + id);
    task.setStatus("COMPLETED");
    task.setSourceReference("pde-opportunity:round-" + id);
    task.setResultJson("{\"decision\":\"APPROVE\"}");
    task.setEvidenceJson("{\"sources\":2}");
    task.setInputTokens(2834L);
    task.setCachedInputTokens(2304L);
    task.setOutputTokens(5861L);
    task.setEstimatedCostUsd(new BigDecimal("0.01347240"));
    task.setCostEstimationStatus("ESTIMATED");
    task.setCreatedAt(Instant.parse("2026-08-20T21:40:00Z"));
    task.setReceivedAt(Instant.parse("2026-08-20T21:40:24Z"));
    task.setDeliveredAt(Instant.parse("2026-08-20T21:41:24Z"));
    task.setUpdatedAt(Instant.parse("2026-08-20T21:41:24Z"));
    task.setExecutionModelCode("gpt-5.4-mini-2026-03-17");
    task.setExecutionReasoningEffort("high");
    task.setExecutionPrompt("Comprove a dor com fontes independentes.");
    return task;
  }
}
