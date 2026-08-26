package com.marketinghub.businessprocess.document.service;

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

/** Responsabilidade: comprovar limite, segregação e auditoria dos documentos de atividades BPM. */
class BusinessProcessActivityDocumentServiceTest {
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityDocumentService service =
      new BusinessProcessActivityDocumentService(processes, tasks, new ObjectMapper());

  /** Retorna no máximo dez documentos recentes com origem, consumo e custo preservados. */
  @Test
  void returnsOnlyTenRecentDocumentsWithAuditData() {
    when(processes.findById(22L)).thenReturn(Optional.of(process()));
    List<AgentTask> generated = LongStream.rangeClosed(1, 11).mapToObj(this::documentTask).toList();
    when(tasks.findRecentActivityDocuments(eq(22L), eq("evidence"), any(Pageable.class)))
        .thenReturn(generated);

    var result = service.recentDocuments(22L, "evidence");

    assertThat(result).hasSize(10);
    assertThat(result.get(0).sourceReference()).isEqualTo("opportunity:1");
    assertThat(result.get(0).assignedAgentNickname()).isEqualTo("Argos");
    assertThat(result.get(0).estimatedCostUsd()).isEqualByComparingTo("0.12000000");
    assertThat(result.get(0).startedAt()).isEqualTo(Instant.parse("2026-08-20T21:40:24Z"));
    assertThat(result.get(0).finishedAt()).isEqualTo(Instant.parse("2026-08-20T21:41:24Z"));
    assertThat(result.get(0).modelCode()).isEqualTo("gpt-5.6-sol");
    assertThat(result.get(0).reasoningEffort()).isEqualTo("high");
    assertThat(result.get(0).promptSent()).isEqualTo("Prompt final enviado ao modelo.");
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(tasks).findRecentActivityDocuments(eq(22L), eq("evidence"), pageable.capture());
    assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
  }

  /** Lista somente identificadores documentais já concluídos da definição solicitada. */
  @Test
  void listsDocumentActivitiesWithinProcess() {
    when(processes.findById(22L)).thenReturn(Optional.of(process()));
    when(tasks.findDocumentActivityIds(22L)).thenReturn(List.of("evidence", "compare"));

    assertThat(service.documentActivityIds(22L)).containsExactly("evidence", "compare");
    verify(tasks).findDocumentActivityIds(22L);
  }

  /** Recupera o modelo das evidências legadas sem atribuir esforço de raciocínio inexistente. */
  @Test
  void recoversLegacyModelFromEvidenceWithoutInventingReasoning() {
    when(processes.findById(22L)).thenReturn(Optional.of(process()));
    AgentTask legacy = documentTask(1L);
    legacy.setExecutionModelCode(null);
    legacy.setExecutionReasoningEffort(null);
    legacy.setEvidenceJson("{\"model\":\"gpt-5.6-sol\"}");
    when(tasks.findRecentActivityDocuments(eq(22L), eq("evidence"), any(Pageable.class)))
        .thenReturn(List.of(legacy));

    var result = service.recentDocuments(22L, "evidence").getFirst();

    assertThat(result.modelCode()).isEqualTo("gpt-5.6-sol");
    assertThat(result.reasoningEffort()).isNull();
  }

  /** Limita o histórico do objetivo do processo aos dez documentos mais recentes. */
  @Test
  void returnsOnlyTenRecentProcessDocuments() {
    when(processes.findById(22L)).thenReturn(Optional.of(process()));
    when(tasks.findRecentProcessDocuments(eq(22L), any(Pageable.class)))
        .thenReturn(LongStream.rangeClosed(1, 11).mapToObj(this::documentTask).toList());

    var result = service.recentProcessDocuments(22L);

    assertThat(result).hasSize(10);
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(tasks).findRecentProcessDocuments(eq(22L), pageable.capture());
    assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
  }

  /** Bloqueia consulta que tenta misturar um identificador inexistente ou não executável. */
  @Test
  void rejectsActivityOutsideProcessTasks() {
    when(processes.findById(22L)).thenReturn(Optional.of(process()));

    assertThatThrownBy(() -> service.recentDocuments(22L, "gate"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Atividade não encontrada");
    verify(tasks, never()).findRecentActivityDocuments(anyLong(), anyString(), any());
  }

  /** Monta uma definição com uma atividade documental e um gate não executável. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition value = new BusinessProcessDefinition();
    value.setId(22L);
    value.setDiagramJson(
        "{\"nodes\":[{\"id\":\"start\",\"type\":\"START\",\"label\":\"Início\"},"
            + "{\"id\":\"evidence\",\"type\":\"TASK\",\"label\":\"Comprovar dor\"},"
            + "{\"id\":\"gate\",\"type\":\"GATEWAY\",\"label\":\"Aprovada?\"},"
            + "{\"id\":\"end\",\"type\":\"END\",\"label\":\"Fim\"}]}");
    return value;
  }

  /** Monta uma tarefa documental concluída para testar o contrato de leitura. */
  private AgentTask documentTask(long id) {
    Agent agent = new Agent();
    agent.setAgentKey("market-radar");
    agent.setNickname("Argos");
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setAssignedAgent(agent);
    task.setTitle("Dossiê " + id);
    task.setSourceReference("opportunity:" + id);
    task.setResultJson("{\"decision\":\"APPROVE\"}");
    task.setEvidenceJson("{\"sources\":2}");
    task.setInputTokens(100L);
    task.setCachedInputTokens(20L);
    task.setOutputTokens(40L);
    task.setEstimatedCostUsd(new BigDecimal("0.12000000"));
    task.setCostEstimationStatus("ESTIMATED");
    task.setReceivedAt(Instant.parse("2026-08-20T21:40:24Z"));
    task.setExecutionModelCode("gpt-5.6-sol");
    task.setExecutionReasoningEffort("high");
    task.setExecutionPrompt("Prompt final enviado ao modelo.");
    task.setDeliveredAt(Instant.parse("2026-08-20T21:41:24Z"));
    task.setUpdatedAt(task.getDeliveredAt());
    return task;
  }
}
