package com.marketinghub.consumerexpectation.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

/** Confere a consulta real do recibo mais recente em banco efêmero, sem simular ordenação. */
class ExpectationMeasurementRepositoryTest {
  /** Garante que bloqueio recente vence sucesso antigo, ignora outros comandos e isola ciclos. */
  @Test
  void latestMeasurementIncludesBlockersAndExcludesOtherCycles() {
    try (var factory =
            new Configuration()
                .addAnnotatedClass(LearningSalesCycleEvent.class)
                .setProperty(
                    "hibernate.connection.url",
                    "jdbc:h2:mem:expectation-receipt-" + UUID.randomUUID())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory();
        var entityManager = factory.createEntityManager()) {
      var repository =
          new JpaRepositoryFactory(entityManager)
              .getRepository(LearningSalesCycleEventRepository.class);
      var blocked = event(501L, 2, "MEASUREMENT_BLOCKED");
      entityManager.getTransaction().begin();
      for (var value :
          List.of(
              event(501L, 1, "MEASURE"),
              blocked,
              event(501L, 3, "OTHER"),
              event(701L, 4, "MEASURE"))) entityManager.persist(value);
      entityManager.getTransaction().commit();
      entityManager.clear();
      var latest =
          repository.findFirstByCycleIdAndActionInOrderByRevisionDesc(
              501L, List.of("MEASURE", "MEASUREMENT_BLOCKED"));
      assertThat(latest).isPresent();
      assertThat(latest.orElseThrow().getId()).isEqualTo(blocked.getId());
      assertThat(
              repository.findFirstByCycleIdAndActionInOrderByRevisionDesc(
                  999L, List.of("MEASURE", "MEASUREMENT_BLOCKED")))
          .isEmpty();
    }
  }

  /** Constrói evento sintético íntegro para gravar e consultar pelo repository oficial. */
  private LearningSalesCycleEvent event(Long cycleId, long revision, String action) {
    var e = new LearningSalesCycleEvent();
    e.setCycleId(cycleId);
    e.setRevision(revision);
    e.setAction(action);
    e.setRequestKey(UUID.randomUUID().toString());
    e.setRequestJson("{}");
    e.setFromStage("MEASUREMENT");
    e.setToStage("MEASUREMENT");
    e.setOperatorName("synthetic");
    e.setSummary("Teste segregado");
    e.setEvidenceReference("synthetic:test");
    e.setEvidenceJson("{}");
    e.setCreatedAt(Instant.parse("2026-11-25T00:00:00Z"));
    return e;
  }
}
