package com.marketinghub.repository.jpa.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentTheme;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agent.AgentThemeRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/** Responsabilidade: validar os contratos de persistência das tarefas do BPM. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class AgentTaskRepositoryDocumentTest {

  @Autowired private AgentTaskRepository tasks;
  @Autowired private AgentRepository agents;
  @Autowired private AgentThemeRepository themes;
  @Autowired private BusinessProcessDefinitionRepository processes;
  @Autowired private EntityManager entityManager;
  @Autowired private org.springframework.transaction.PlatformTransactionManager transactions;

  /** Prova que prontidão usa resultados escalares e ignora auditoria volumosa e outras origens. */
  @Test
  void functionalSnapshotsDoNotHydrateAuditAndPreserveExactScope() {
    var agent = persistedAgent();
    var process = persistedProcess();
    var now = Instant.parse("2026-09-12T08:00:00Z");
    var current = task(agent, process, "communicationContract", "COMPLETED", "{\"proof\":1}", now);
    current.setSourceReference("experiment:91092");
    current.setExecutionPrompt("auditoria-sintetica-".repeat(220000));
    tasks.save(current);
    var older =
        task(
            agent,
            process,
            "communicationContract",
            "COMPLETED",
            "{\"proof\":0}",
            now.minusSeconds(86400));
    older.setSourceReference("experiment:91092");
    tasks.save(older);
    var another = task(agent, process, "communicationContract", "IN_PROGRESS", null, now);
    another.setSourceReference("experiment:91093");
    tasks.save(another);
    entityManager.flush();
    entityManager.clear();

    var summary =
        tasks.findFunctionalSnapshots(
            "experiment:91092", java.util.Set.of(process.getProcessCode()), now.minusSeconds(3600));
    assertThat(summary).hasSize(1);
    assertThat(summary.getFirst().id()).isEqualTo(current.getId());
    assertThat(summary.getFirst().resultJson()).isEqualTo("{\"proof\":1}");
    assertThat(summary.getFirst().agentKey()).isEqualTo(agent.getAgentKey());
    assertThat(entityManager.unwrap(org.hibernate.Session.class).getStatistics().getEntityCount())
        .isZero();
    assertThat(
            tasks.findFunctionalSnapshotsByProcessSince(
                process.getId(), "experiment:91092", now.minusSeconds(3600)))
        .containsExactlyElementsOf(summary);
    assertThat(
            tasks.findFunctionalSnapshotsByProcessSince(process.getId(), "experiment:91092", null))
        .extracting(com.marketinghub.agenttask.AgentTaskFunctionalSnapshot::id)
        .containsExactly(current.getId(), older.getId());
    assertThat(
            tasks.findFunctionalSnapshotsByProcessSince(process.getId(), "experiment:91094", null))
        .isEmpty();
    assertThat(
            tasks.findFunctionalSnapshots(
                "experiment:91092", java.util.Set.of("outro-processo"), null))
        .isEmpty();
    assertThat(entityManager.unwrap(org.hibernate.Session.class).getStatistics().getEntityCount())
        .isZero();
    assertThat(
            tasks.existsByProcessDefinitionIdAndSourceReferenceAndProcessActivityIdAndStatusIn(
                process.getId(),
                "experiment:91092",
                "communicationContract",
                java.util.Set.of("PENDING", "IN_PROGRESS")))
        .isFalse();
    assertThat(
            tasks.existsByProcessDefinitionIdAndSourceReferenceAndProcessActivityIdAndStatusIn(
                process.getId(),
                "experiment:91093",
                "communicationContract",
                java.util.Set.of("PENDING", "IN_PROGRESS")))
        .isTrue();
    assertThat(
            tasks.findBySourceReferenceAndProcessDefinitionProcessCodeOrderByCreatedAtAscIdAsc(
                "experiment:91092", process.getProcessCode()))
        .extracting(AgentTask::getId)
        .containsExactly(older.getId(), current.getId());
    assertThat(
            tasks
                .findBySourceReferenceStartingWithAndProcessDefinitionProcessCodeOrderByUpdatedAtDescIdDesc(
                    "experiment:9109", "outro-processo"))
        .isEmpty();
  }

  /** Comprova o retorno à produção usando o repositório real, inclusive sem corte de data. */
  @Test
  void reopensCreativeBriefUsingPersistedHistoryWithoutDateFilter() {
    var agent = persistedAgent();
    var process = persistedProcess();
    process.setProcessCode("creative-production-approval");
    processes.save(process);
    var now = Instant.parse("2026-09-12T08:00:00Z");
    var brief =
        task(
            agent,
            process,
            "nonAudiovisual",
            "COMPLETED",
            "{\"functionalOutput\":{\"staticAssets\":[{}]}}",
            now);
    brief.setSourceReference("experiment:91092");
    tasks.saveAndFlush(brief);
    entityManager.clear();
    var provider =
        new com.marketinghub.communication.v1.CreativeProductionReadinessProvider(
            tasks,
            org.mockito.Mockito.mock(
                com.marketinghub.businessprocess.execution.service.predecessor
                    .ProductProcessActivityPredecessorService.class),
            new com.fasterxml.jackson.databind.ObjectMapper());
    var activity = new com.marketinghub.businessprocess.BusinessProcessActivityDefinition();
    activity.setActivityId("nonAudiovisual");
    var product = com.marketinghub.product.Product.builder().id(91004L).build();

    assertThat(provider.requiresFreshExecution(process, activity, product, "experiment:91092"))
        .isTrue();
    assertThat(provider.requiresFreshExecution(process, activity, product, "experiment:91093"))
        .isFalse();
    assertThat(entityManager.unwrap(org.hibernate.Session.class).getStatistics().getEntityCount())
        .isZero();
  }

  /**
   * Comprova no banco que dois callbacks concorrentes observam o commit antes de repetir efeitos.
   */
  @Test
  @org.springframework.transaction.annotation.Transactional(
      propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void serializesConcurrentCallbackReceipts() throws Exception {
    var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
    AgentTask created =
        transaction.execute(
            status ->
                tasks.saveAndFlush(
                    task(
                        persistedAgent(),
                        persistedProcess(),
                        "marketStrategy",
                        "IN_PROGRESS",
                        null,
                        Instant.now())));
    Long id = created.getId();
    var locked = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    var competing = new java.util.concurrent.CountDownLatch(1);
    var effects = new java.util.concurrent.atomic.AtomicInteger();
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () ->
                  transaction.execute(
                      status -> {
                        var current = tasks.findLockedById(id).orElseThrow();
                        locked.countDown();
                        awaitLatch(release);
                        current.setStatus("COMPLETED");
                        effects.incrementAndGet();
                        return tasks.save(current).getId();
                      }));
      try {
        assertThat(locked.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        var second =
            pool.submit(
                () ->
                    transaction.execute(
                        status -> {
                          competing.countDown();
                          var current = tasks.findLockedById(id).orElseThrow();
                          if ("IN_PROGRESS".equals(current.getStatus())) effects.incrementAndGet();
                          return current.getStatus();
                        }));
        assertThat(competing.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> second.get(100, java.util.concurrent.TimeUnit.MILLISECONDS))
            .isInstanceOf(java.util.concurrent.TimeoutException.class);
        release.countDown();
        first.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(second.get(5, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo("COMPLETED");
        assertThat(effects.get()).isEqualTo(1);
      } finally {
        release.countDown();
      }
    } finally {
      transaction.executeWithoutResult(
          status -> {
            var current = tasks.findById(id).orElseThrow();
            var process = current.getProcessDefinition();
            var agent = current.getAssignedAgent();
            var theme = agent.getTheme();
            tasks.delete(current);
            tasks.flush();
            processes.delete(process);
            agents.delete(agent);
            agents.flush();
            themes.delete(theme);
          });
    }
  }

  /** Aguarda a sincronização controlada e conserva interrupção como falha explícita do teste. */
  private static void awaitLatch(java.util.concurrent.CountDownLatch latch) {
    try {
      if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS))
        throw new IllegalStateException("Timeout da fixture concorrente");
    } catch (InterruptedException ex) {
      org.slf4j.LoggerFactory.getLogger(AgentTaskRepositoryDocumentTest.class)
          .error("Fixture de callback interrompida", ex);
      Thread.currentThread().interrupt();
      throw new IllegalStateException(ex);
    }
  }

  /** Garante segregação, ordenação e limite de dez documentos concluídos por atividade. */
  @Test
  void findsOnlyTenMostRecentCompletedDocumentsFromRequestedActivity() {
    Agent agent = persistedAgent();
    BusinessProcessDefinition process = persistedProcess();
    Instant newest = Instant.parse("2026-08-20T20:00:00Z");
    IntStream.range(0, 12)
        .forEach(
            index ->
                tasks.save(
                    task(
                        agent,
                        process,
                        "evidence",
                        "COMPLETED",
                        "{\"ordem\":" + index + "}",
                        newest.minusSeconds(index))));
    tasks.save(task(agent, process, "evidence", "PENDING", "{\"ignorar\":true}", newest));
    tasks.save(task(agent, process, "compare", "COMPLETED", "{\"outra\":true}", newest));
    tasks.save(task(agent, process, "evidence", "COMPLETED", "", newest));
    entityManager.flush();
    entityManager.clear();

    var documents =
        tasks.findRecentActivityDocuments(process.getId(), "evidence", PageRequest.of(0, 10));

    assertThat(documents).hasSize(10);
    assertThat(documents.getFirst().getResultJson()).isEqualTo("{\"ordem\":0}");
    assertThat(documents.getLast().getResultJson()).isEqualTo("{\"ordem\":9}");
    assertThat(tasks.findDocumentActivityIds(process.getId()))
        .containsExactlyInAnyOrder("evidence", "compare");
    assertThat(tasks.findRecentProcessDocuments(process.getId(), PageRequest.of(0, 10)))
        .hasSize(10);
  }

  /** Persiste o agente mínimo responsável pelas execuções documentais do teste. */
  private Agent persistedAgent() {
    AgentTheme theme = themes.save(AgentTheme.builder().name("Pesquisa documental").build());
    return agents.save(
        Agent.builder()
            .theme(theme)
            .name("Radar de oportunidades")
            .nickname("Argos documental")
            .agentKey("market-radar-document-test")
            .status("READY")
            .executionMode("MANAGED")
            .build());
  }

  /** Persiste uma definição simples usada para segregar o histórico documental. */
  private BusinessProcessDefinition persistedProcess() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("PDE-DOCUMENT-TEST");
    process.setName("Descoberta documental");
    process.setPurpose("Produzir evidências");
    process.setOwnerName("Marketing Hub");
    process.setTriggerDescription("Sinal recebido");
    process.setOutcomeDescription("Dossiê entregue");
    process.setVersionNumber(1);
    process.setStatus("PUBLISHED");
    process.setDiagramJson("{\"nodes\":[],\"flows\":[]}");
    process.setCreatedAt(Instant.parse("2026-08-20T19:00:00Z"));
    return processes.save(process);
  }

  /** Monta uma tarefa com os campos obrigatórios e o resultado informado. */
  private AgentTask task(
      Agent agent,
      BusinessProcessDefinition process,
      String activityId,
      String status,
      String resultJson,
      Instant deliveredAt) {
    AgentTask task = new AgentTask();
    task.setAssignedAgent(agent);
    task.setRequestedByType("SYSTEM");
    task.setRequestedByName("BPM");
    task.setTitle("Documento " + deliveredAt);
    task.setDescription("Produzir documento auditável");
    task.setPriority("MEDIUM");
    task.setStatus(status);
    task.setProcessDefinition(process);
    task.setProcessActivityId(activityId);
    task.setProcessActivityName("Produzir documento");
    task.setTaskKind("REGULAR");
    task.setResultJson(resultJson);
    task.setDeliveredAt(deliveredAt);
    task.setCreatedAt(deliveredAt.minusSeconds(60));
    task.setUpdatedAt(deliveredAt);
    return task;
  }
}
