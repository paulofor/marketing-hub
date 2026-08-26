package com.marketinghub.repository.jpa.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentTheme;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/** Responsabilidade: comprovar a segregação e a ordenação da consulta recente de tarefas BPM. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class AgentTaskRecentActivityExecutionRepositoryTest {
  @Autowired private TestEntityManager entityManager;
  @Autowired private AgentTaskRepository repository;

  /** Consulta versões do mesmo processo sem misturar outra atividade ou outro processo. */
  @Test
  void findsRecentActivityExecutionsAcrossVersionsWithoutCrossProcessLeakage() {
    Agent argos = agent();
    BusinessProcessDefinition version1 = process("pde-opportunity-discovery", 1);
    BusinessProcessDefinition version4 = process("pde-opportunity-discovery", 4);
    BusinessProcessDefinition anotherProcess = process("another-process", 1);
    AgentTask older = task(argos, version1, "evidence", 1, "2026-08-20T10:00:00Z");
    AgentTask newer = task(argos, version4, "evidence", 2, "2026-08-21T10:00:00Z");
    task(argos, version4, "questions", 3, "2026-08-22T10:00:00Z");
    task(argos, anotherProcess, "evidence", 4, "2026-08-23T10:00:00Z");
    entityManager.flush();
    entityManager.clear();

    var result =
        repository.findRecentActivityExecutions(
            "pde-opportunity-discovery", "evidence", PageRequest.of(0, 10));

    assertThat(result).extracting(AgentTask::getId).containsExactly(newer.getId(), older.getId());
    assertThat(result)
        .extracting(task -> task.getProcessDefinition().getVersionNumber())
        .containsExactly(4, 1);
  }

  /** Persiste a identidade mínima do Argos exigida pelas tarefas. */
  private Agent agent() {
    AgentTheme theme = new AgentTheme();
    theme.setName("Pesquisa");
    entityManager.persist(theme);
    Agent agent = new Agent();
    agent.setTheme(theme);
    agent.setName("Radar de mercado");
    agent.setNickname("Argos");
    agent.setAgentKey("market-radar");
    agent.setStatus("ACTIVE");
    agent.setCurrentVersion(1);
    agent.setExecutionMode("WORKER");
    agent.setAutomaticExecutionEnabled(true);
    return entityManager.persist(agent);
  }

  /** Persiste uma versão mínima de processo para vincular a execução. */
  private BusinessProcessDefinition process(String processCode, int version) {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode(processCode);
    process.setName(processCode + " v" + version);
    process.setPurpose("Comprovar oportunidade.");
    process.setOwnerName("Inteligência");
    process.setTriggerDescription("Sinal de mercado.");
    process.setOutcomeDescription("Decisão registrada.");
    process.setVersionNumber(version);
    process.setStatus("RETIRED");
    process.setProcessType("VALUE_PROCESS");
    process.setDiagramJson("{\"nodes\":[],\"flows\":[]}");
    process.setCreatedAt(Instant.parse("2026-08-20T09:00:00Z"));
    return entityManager.persist(process);
  }

  /** Persiste uma tarefa com o instante necessário para testar a ordem recente. */
  private AgentTask task(
      Agent agent,
      BusinessProcessDefinition process,
      String activityId,
      int suffix,
      String createdAt) {
    AgentTask task = new AgentTask();
    task.setAssignedAgent(agent);
    task.setRequestedByType("HUMAN");
    task.setRequestedByName("Operação");
    task.setTitle("Tarefa " + suffix);
    task.setDescription("Comprovar dor.");
    task.setPriority("HIGH");
    task.setStatus("COMPLETED");
    task.setProcessDefinition(process);
    task.setProcessActivityId(activityId);
    task.setProcessActivityName(activityId);
    task.setExceptional(false);
    task.setTaskKind("ACTIVITY");
    task.setCostEstimationStatus("NOT_REPORTED");
    task.setCreatedAt(Instant.parse(createdAt));
    task.setUpdatedAt(Instant.parse(createdAt));
    return entityManager.persist(task);
  }
}
