package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/** Responsabilidade: provar que gravações financeiras não apagam bloqueios funcionais do ciclo. */
class LearningCycleFinancialEventIsolationTest {
  /** Exercita o filtro JPA real com bloqueio anterior e três eventos financeiros posteriores. */
  @Test
  void keepsLastFunctionalEventAfterWorksheetAndBudgetChanges() {
    var database =
        new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .generateUniqueName(true)
            .build();
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(database);
    factory.setManagedTypes(PersistenceManagedTypes.of(LearningSalesCycleEvent.class.getName()));
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
    factory.afterPropertiesSet();
    var manager = factory.getObject().createEntityManager();
    try {
      manager.getTransaction().begin();
      var actions =
          List.of(
              "MEASUREMENT_BLOCKED",
              "SAVE_COST_WORKSHEET",
              "APPROVE_COST_CEILING",
              "AUTHORIZE_PREPARATION_BUDGET",
              "AUTHORIZE_VIDEO_BUDGET");
      for (int i = 0; i < actions.size(); i++) {
        var event = new LearningSalesCycleEvent();
        event.setCycleId(700L);
        event.setRevision(i + 1);
        event.setRequestKey(UUID.randomUUID().toString());
        event.setRequestJson("{}");
        event.setEvidenceJson("{}");
        event.setEvidenceReference("internal://fixture/financial-event/" + i);
        event.setAction(actions.get(i));
        event.setFromStage("MEASUREMENT");
        event.setToStage("MEASUREMENT");
        event.setOperatorName("Teste local");
        event.setSummary("Fonte comercial ausente; nenhuma liberação");
        event.setCreatedAt(Instant.now());
        manager.persist(event);
      }
      manager.getTransaction().commit();
      var repository =
          new JpaRepositoryFactory(manager).getRepository(LearningSalesCycleEventRepository.class);
      assertThat(repository.findFirstByCycleIdOrderByRevisionDesc(700L).orElseThrow().getAction())
          .isEqualTo("AUTHORIZE_VIDEO_BUDGET");
      assertThat(
              repository
                  .findFirstByCycleIdAndActionNotInOrderByRevisionDesc(
                      700L, actions.subList(1, actions.size()))
                  .orElseThrow()
                  .getAction())
          .isEqualTo("MEASUREMENT_BLOCKED");
      assertThat(
              repository.findFirstByCycleIdAndActionNotInOrderByRevisionDesc(
                  701L, actions.subList(1, actions.size())))
          .isEmpty();
    } finally {
      manager.close();
      factory.destroy();
      database.shutdown();
    }
  }
}
