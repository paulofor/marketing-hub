package com.marketinghub.product.bigidea.v1.service;

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experimentstrategist.*;
import com.marketinghub.experimentstrategist.service.*;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.experimentstrategist.*;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

/** Simula somente cadastro e fila de Atena, mantendo controllers, validação e adoção produtivos. */
@TestConfiguration
@Profile("big-idea-fixture")
@Import({BigIdeaResearch.class, BigIdeaResearchLocalFixture.Completion.class})
public class BigIdeaResearchLocalFixture {
  /** Mantém as execuções sintéticas separadas das tabelas e credenciais produtivas. */
  @Bean
  ExperimentStrategistExecutionRepository researchExecutions(ObjectMapper json) {
    var repository = mock(ExperimentStrategistExecutionRepository.class);
    var records = new ConcurrentHashMap<Long, ExperimentStrategistExecution>();
    var sequence = new AtomicLong(99000);
    when(repository.findById(anyLong()))
        .thenAnswer(i -> Optional.ofNullable(records.get(i.getArgument(0))));
    when(repository.findLatestBigIdea(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              return records.values().stream()
                  .filter(
                      e -> {
                        try {
                          return json.readTree(e.getEvidenceSnapshot())
                                  .path("bigIdeaResearch")
                                  .path("productId")
                                  .asLong()
                              == id;
                        } catch (Exception ex) {
                          org.slf4j.LoggerFactory.getLogger(BigIdeaResearchLocalFixture.class)
                              .error("Contexto sintético inválido. executionId={}", e.getId(), ex);
                          throw new AssertionError("Contexto sintético inválido", ex);
                        }
                      })
                  .max(Comparator.comparing(ExperimentStrategistExecution::getId));
            });
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              ExperimentStrategistExecution e = i.getArgument(0);
              e.setId(sequence.incrementAndGet());
              records.put(e.getId(), e);
              return e;
            });
    when(repository.save(any()))
        .thenAnswer(
            i -> {
              ExperimentStrategistExecution e = i.getArgument(0);
              records.put(e.getId(), e);
              return e;
            });
    return repository;
  }

  /** Oferece vínculo sintético a um plano sem reexecutar o planejamento. */
  @Bean
  CommercialPlanRepository researchPlans() {
    var repository = mock(CommercialPlanRepository.class);
    when(repository.findByProductId(anyLong()))
        .thenAnswer(
            i -> {
              var plan = new CommercialPlan();
              plan.setId(i.getArgument(0));
              return List.of(plan);
            });
    return repository;
  }

  /** Recebe o resultado simulado pelo mesmo validador usado pelo callback produtivo. */
  @RestController
  static class Completion {
    private final ExperimentStrategistExecutionRepository executions;
    private final ExperimentStrategistExecutionService service;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    /** Injeta a fila sintética e a validação real, sem eventos de operação comercial. */
    Completion(
        ExperimentStrategistExecutionRepository executions,
        BigIdeaResearch research,
        ObjectMapper json,
        javax.sql.DataSource source) {
      this.executions = executions;
      this.jdbc = new org.springframework.jdbc.core.JdbcTemplate(source);
      service =
          new ExperimentStrategistExecutionService(
              executions,
              mock(ExperimentStrategistBehavioralSnapshotRepository.class),
              mock(CommercialPlanService.class),
              mock(ExperimentStrategistContextService.class),
              json,
              event -> {});
      org.springframework.test.util.ReflectionTestUtils.setField(
          service, "bigIdeaResearch", research);
    }

    /** Simula claim e callback, preservando toda a validação de conteúdo e custo. */
    @PostMapping("/fixture/research/{id}/complete")
    Object complete(@PathVariable Long id, @RequestBody String body) {
      executions.findById(id).orElseThrow().setStatus(ExperimentStrategistExecutionStatus.RUNNING);
      return service.complete(
          id,
          new ExperimentStrategistExecutionService.CompleteRequest(
              "[]", body, "[]", body, "modelo-sintetico-local", new java.math.BigDecimal("0.01")));
    }

    /** Executa as consultas nativas reais em MySQL 5.7, isolando ideia e parecer estratégico. */
    @GetMapping("/fixture/research/queries")
    Object queries() throws ReflectiveOperationException {
      jdbc.execute(
          "CREATE TABLE IF NOT EXISTS experiment_strategist_execution(id BIGINT PRIMARY KEY,commercial_plan_id BIGINT,authority_mode VARCHAR(50),evidence_snapshot LONGTEXT,created_at DATETIME NOT NULL) ENGINE=InnoDB");
      for (long id : new long[] {99181, 99182, 99183})
        jdbc.update(
            "INSERT IGNORE INTO experiment_strategist_execution VALUES(?,99001,?,?,UTC_TIMESTAMP())",
            id,
            id == 99181 ? "READ_ONLY_RESEARCH" : BigIdeaResearch.MODE,
            "{\"bigIdeaResearch\":{\"productId\":" + (id == 99183 ? 99288 : 99101) + "}}");
      String latest =
          ExperimentStrategistExecutionRepository.class
              .getMethod("findLatestBigIdea", Long.class)
              .getAnnotation(org.springframework.data.jpa.repository.Query.class)
              .value()
              .replace("?1", "?");
      String strategy =
          ExperimentStrategistExecutionRepository.class
              .getMethod("findFirstByCommercialPlanIdOrderByCreatedAtDesc", Long.class)
              .getAnnotation(org.springframework.data.jpa.repository.Query.class)
              .value()
              .replace("?1", "?");
      return Map.of(
          "product",
          jdbc.queryForList(latest, 99101),
          "otherProduct",
          jdbc.queryForList(latest, 99288),
          "strategy",
          jdbc.queryForList(strategy, 99001));
    }
  }
}
