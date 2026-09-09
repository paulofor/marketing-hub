package com.marketinghub.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.settings.GeneralSetting;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

/** Comprova que contextos locais não compartilham dados nem removem o schema uns dos outros. */
class TestDatabaseIsolationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withInitializer(new ConfigDataApplicationContextInitializer())
          .withConfiguration(
              AutoConfigurations.of(
                  DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
          .withUserConfiguration(IsolatedModel.class)
          .withPropertyValues(
              "spring.config.location=classpath:/application-test.yml",
              "spring.profiles.active=test");

  /** Fechar um contexto antigo com create-drop preserva as tabelas e os dados do contexto ativo. */
  @Test
  void closingEarlierContextPreservesActiveSchema() {
    runner.run(
        earlier ->
            runner.run(
                active -> {
                  assertThat(earlier).hasNotFailed();
                  assertThat(active).hasNotFailed();
                  var database = new JdbcTemplate(active.getBean(DataSource.class));
                  database.update("insert into general_setting (name) values ('fixture-active')");
                  earlier.getSourceApplicationContext().close();
                  assertThat(
                          database.queryForObject(
                              "select count(*) from general_setting where name = 'fixture-active'",
                              Integer.class))
                      .isEqualTo(1);
                }));
  }

  /** Um dado persistido depois de abrir os dois contextos permanece exclusivo da sua origem. */
  @Test
  void simultaneousContextsDoNotShareRecords() {
    runner.run(
        first ->
            runner.run(
                second -> {
                  assertThat(first).hasNotFailed();
                  assertThat(second).hasNotFailed();
                  var source = new JdbcTemplate(first.getBean(DataSource.class));
                  var other = new JdbcTemplate(second.getBean(DataSource.class));
                  source.update("insert into general_setting (name) values ('fixture-isolated')");
                  assertThat(
                          other.queryForObject(
                              "select count(*) from general_setting", Integer.class))
                      .isZero();
                  assertThat(
                          source.queryForObject(
                              "select count(*) from general_setting", Integer.class))
                      .isEqualTo(1);
                }));
  }

  /** Limita a reprodução ao JPA real, sem agentes, APIs externas ou banco produtivo. */
  @TestConfiguration(proxyBeanMethods = false)
  static class IsolatedModel {
    /** Reutiliza uma entidade canônica simples para exercer a criação e remoção reais do schema. */
    @Bean
    PersistenceManagedTypes persistenceManagedTypes() {
      return PersistenceManagedTypes.of(GeneralSetting.class.getName());
    }
  }
}
