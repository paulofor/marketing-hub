package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import java.time.Instant;
import java.util.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

/**
 * Responsabilidade: validar a consulta financeira real por linhagem e ordem global no banco local.
 */
class LearningCycleBudgetHistoryRepositoryTest {
  /** Executa o repositório JPA real sem misturar ações, ciclos alheios ou revisões locais. */
  @Test
  void queriesOnlyLineageAndOrdersByDateThenIdInsteadOfCycleRevision() {
    try (var factory =
            new Configuration()
                .addAnnotatedClass(LearningSalesCycleEvent.class)
                .setProperty(
                    "hibernate.connection.url", "jdbc:h2:mem:budget-lineage-" + UUID.randomUUID())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory();
        var manager = factory.createEntityManager()) {
      var repository =
          new JpaRepositoryFactory(manager).getRepository(LearningSalesCycleEventRepository.class);
      manager.getTransaction().begin();
      var old = event(1900L, 90, "APPROVE_COST_CEILING", "2026-11-01T00:00:00Z");
      var next = event(2000L, 1, "APPROVE_COST_CEILING", "2026-11-02T00:00:00Z");
      var tie = event(2000L, 2, "APPROVE_COST_CEILING", "2026-11-02T00:00:00Z");
      for (var value :
          List.of(
              old,
              next,
              tie,
              event(9999L, 99, "APPROVE_COST_CEILING", "2026-11-03T00:00:00Z"),
              event(2000L, 3, "OTHER_ACTION", "2026-11-03T00:00:00Z"))) manager.persist(value);
      manager.getTransaction().commit();
      assertThat(
              repository.findByCycleIdInAndActionOrderByCreatedAtDescIdDesc(
                  List.of(1900L, 2000L), "APPROVE_COST_CEILING"))
          .extracting(LearningSalesCycleEvent::getId)
          .containsExactly(tie.getId(), next.getId(), old.getId());
    }
  }

  /** Cria somente eventos sintéticos com campos canônicos exigidos pela persistência. */
  private LearningSalesCycleEvent event(Long cycleId, long revision, String action, String at) {
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycleId);
    event.setRequestKey(UUID.randomUUID().toString());
    event.setRequestJson("{}");
    event.setEvidenceJson("{}");
    event.setRevision(revision);
    event.setFromStage("PLANNING");
    event.setToStage("PLANNING");
    event.setAction(action);
    event.setOperatorName("Teste local");
    event.setSummary("Evento financeiro sintético");
    event.setEvidenceReference("internal://local-test");
    event.setCreatedAt(Instant.parse(at));
    return event;
  }
}
