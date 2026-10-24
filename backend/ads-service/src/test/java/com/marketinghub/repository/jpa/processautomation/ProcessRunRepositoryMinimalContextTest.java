package com.marketinghub.repository.jpa.processautomation;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.ProcessRunEvent;
import java.util.UUID;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

/** Responsabilidade: preservar o contexto JPA mínimo utilizado pelo harness dos executores. */
class ProcessRunRepositoryMinimalContextTest {
  /** Inicializa e consulta o repositório real sem depender de entidades externas ao coordenador. */
  @Test
  void initializesRepositoryWithOnlyExecutionEntities() {
    try (var factory =
            new Configuration()
                .addAnnotatedClass(ProcessRun.class)
                .addAnnotatedClass(ProcessRunEvent.class)
                .setProperty(
                    "hibernate.connection.url", "jdbc:h2:mem:process-minimal-" + UUID.randomUUID())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "false")
                .buildSessionFactory();
        var manager = factory.createEntityManager()) {
      var repository = new JpaRepositoryFactory(manager).getRepository(ProcessRunRepository.class);
      assertThat(repository.findAll()).isEmpty();
    }
  }
}
