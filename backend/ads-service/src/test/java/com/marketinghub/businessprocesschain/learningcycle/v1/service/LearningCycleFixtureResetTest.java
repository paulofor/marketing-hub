package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningCycleDecisionProposalRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Responsabilidade: provar commit e rollback da limpeza completa, com transação JDBC real. */
class LearningCycleFixtureResetTest {
  /** Confirma que uma falha após remover propostas desfaz as alterações anteriores. */
  @Test
  void rollbackRestoresCleanupWhenNextDeletionFails() {
    try (var context = new AnnotationConfigApplicationContext(FixtureConfiguration.class)) {
      var events = context.getBean(LearningSalesCycleEventRepository.class);
      doThrow(new IllegalStateException("Falha de limpeza simulada"))
          .when(events)
          .deleteAllInBatch();
      assertThrows(
          IllegalStateException.class,
          () -> context.getBean(LearningCycleLocalApplication.FixtureController.class).reset());
      assertEquals(
          0,
          context
              .getBean(JdbcTemplate.class)
              .queryForObject("SELECT COUNT(*) FROM reset_probe", Integer.class));
    }
  }

  /** Confirma o commit somente depois de a limpeza completa retornar com sucesso. */
  @Test
  void commitsSuccessfulCleanup() {
    try (var context = new AnnotationConfigApplicationContext(FixtureConfiguration.class)) {
      assertEquals(
          true,
          context
              .getBean(LearningCycleLocalApplication.FixtureController.class)
              .reset()
              .get("reset"));
      assertEquals(
          1,
          context
              .getBean(JdbcTemplate.class)
              .queryForObject("SELECT COUNT(*) FROM reset_probe", Integer.class));
    }
  }

  /**
   * Responsabilidade: isolar os repositórios simulados e a prova transacional em banco exclusivo.
   */
  @Configuration
  @EnableTransactionManagement
  static class FixtureConfiguration {
    /** Cria um banco próprio por contexto e o encerra ao terminar cada teste. */
    @Bean(destroyMethod = "shutdown")
    EmbeddedDatabase database() {
      return new EmbeddedDatabaseBuilder()
          .generateUniqueName(true)
          .setType(EmbeddedDatabaseType.H2)
          .build();
    }

    /** Usa transações reais para detectar limpeza parcial, sem acessar dados de produto. */
    @Bean
    DataSourceTransactionManager transactionManager(EmbeddedDatabase database) {
      return new DataSourceTransactionManager(database);
    }

    /** Prepara a tabela de prova na conexão real do contexto. */
    @Bean
    JdbcTemplate jdbc(EmbeddedDatabase database) {
      var jdbc = new JdbcTemplate(database);
      jdbc.execute("CREATE TABLE reset_probe (id INTEGER)");
      return jdbc;
    }

    /** Permite provocar uma falha depois da primeira escrita da limpeza. */
    @Bean
    LearningSalesCycleEventRepository events() {
      return mock(LearningSalesCycleEventRepository.class);
    }

    /** Exercita o controller real; a primeira remoção produz uma alteração JDBC verificável. */
    @Bean
    LearningCycleLocalApplication.FixtureController controller(
        JdbcTemplate jdbc, LearningSalesCycleEventRepository events) {
      var proposals = mock(LearningCycleDecisionProposalRepository.class);
      doAnswer(
              invocation -> {
                jdbc.update("INSERT INTO reset_probe VALUES (1)");
                return null;
              })
          .when(proposals)
          .deleteAllInBatch();
      return new LearningCycleLocalApplication.FixtureController(
          mock(BusinessProcessDefinitionRepository.class),
          mock(BusinessProcessActivityDefinitionRepository.class),
          mock(BusinessProcessActivityInstanceRepository.class),
          new ObjectMapper(),
          mock(LearningSalesCycleRepository.class),
          events,
          proposals,
          mock(com.marketinghub.repository.jpa.processautomation.ProcessRunRepository.class),
          mock(com.marketinghub.repository.jpa.processautomation.ProcessRunEventRepository.class));
    }
  }
}
