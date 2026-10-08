package com.marketinghub.pde.vega.privateprototype.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.repository.jpa.vega.VegaAdjustmentExecutionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Responsabilidade: comprovar a fila real do Vega com as collations distintas do MySQL publicado.
 */
@SpringBootTest(
    classes = VegaPrivateQueueMysql57Test.QueueConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "spring.config.location=optional:classpath:vega380/no-production.properties",
      "spring.liquibase.change-log=classpath:vega380/changelog.yaml",
      "integrations.pde-platform.internal-token=vega-local-internal-only",
      "logging.level.root=WARN"
    })
@ActiveProfiles("vega380-local")
@EnabledIfEnvironmentVariable(
    named = "MIRA_CONTROLS_DB_HOST",
    matches = "127\\.0\\.0\\.1|sandbox-docker")
class VegaPrivateQueueMysql57Test {
  /** Reutiliza a aplicação mínima e os repositories oficiais sem carregar serviços produtivos. */
  @Configuration(proxyBeanMethods = false)
  @Profile("vega380-local")
  @Import(VegaPrivateLocalApplication.class)
  static class QueueConfiguration {}

  @Autowired private JdbcTemplate jdbc;
  @Autowired private VegaAdjustmentExecutionRepository executions;
  private static final String VERSION = "musa-pde-entry-v12-primeiro-ajuste-aplicavel";

  /** Reutiliza a engine efêmera com schema próprio do Vega, recusando destinos externos. */
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    String host = System.getenv("MIRA_CONTROLS_DB_HOST");
    String port = System.getenv().getOrDefault("MIRA_CONTROLS_DB_PORT", "3306");
    String schema = "vega_queue_local";
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host)
        || !Set.of("3306", "18316").contains(port))
      throw new IllegalArgumentException("A regressão exige o MySQL efêmero local.");
    properties.add(
        "spring.datasource.url",
        () ->
            "jdbc:mysql://"
                + host
                + ":"
                + port
                + "/"
                + schema
                + "?useSSL=false&serverTimezone=UTC");
    properties.add(
        "spring.datasource.username",
        () -> System.getenv().getOrDefault("MIRA_CONTROLS_DB_USER", "vega"));
    properties.add(
        "spring.datasource.password",
        () -> System.getenv().getOrDefault("MIRA_CONTROLS_DB_PASSWORD", "vega-local-only"));
  }

  /**
   * Recria identidades próprias, incluindo versão diferente apenas por caixa e ciclos distintos.
   */
  @BeforeEach
  void fixtures() {
    cleanup();
    for (int id = 91961; id <= 91969; id++) {
      jdbc.update(
          "INSERT INTO learning_sales_cycle_v1 SELECT ?,product_id,chain_definition_id,chain_code,process_definition_id,?,NULL,?,creation_json,brief_json,inherited_learning_json,stage,?, ?,budget_limit_brl,window_start,window_end,NULL,NULL,NULL,NULL,revision,baseline,created_at,updated_at,version_changed_at,NULL FROM learning_sales_cycle_v1 WHERE id=91002",
          id,
          id + 100,
          "queue-mysql-" + id,
          id == 91963 ? "CLOSED" : "OPEN",
          id == 91964 ? VERSION.toUpperCase(java.util.Locale.ROOT) : VERSION);
      jdbc.update(
          "INSERT INTO vega_private_session_v1(id,cycle_id,product_id,experiment_id,prototype_version,origin,revoked,expires_at,created_at,state,events_json) VALUES(?,?,91004,?,?,?, ?,DATE_ADD(NOW(),INTERVAL ? DAY),NOW(),'GENERATING','{}')",
          "queue-mysql-" + id,
          id,
          id + 100,
          VERSION,
          id == 91967 ? "HUMAN" : "AGENT_VALIDATION",
          id == 91965,
          id == 91966 ? -1 : 1);
      jdbc.update(
          "INSERT INTO vega_adjustment_execution_v1(id,session_id,status,input_json,created_at,lease_until) VALUES(?,?,?,'{}',NOW(),DATE_ADD(NOW(),INTERVAL ? DAY))",
          id,
          "queue-mysql-" + id,
          id >= 91968 ? "RUNNING" : "QUEUED",
          id == 91968 ? -1 : 1);
    }
  }

  /** Remove exclusivamente as identidades desta regressão, preservando as demais fixtures. */
  @AfterEach
  void cleanup() {
    jdbc.update("DELETE FROM vega_adjustment_execution_v1 WHERE id BETWEEN 91961 AND 91969");
    jdbc.update("DELETE FROM vega_private_session_v1 WHERE id LIKE 'queue-mysql-%'");
    jdbc.update("DELETE FROM learning_sales_cycle_v1 WHERE id BETWEEN 91961 AND 91969");
  }

  /**
   * Comprova consulta JPA, paginação, dois ciclos e recusa de versão, vigência e acesso inválidos.
   */
  @Test
  void mixedCollationsPreserveExactVersionAndActiveContexts() {
    assertThat(
            jdbc.queryForObject(
                "SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='learning_sales_cycle_v1' AND COLUMN_NAME='product_version'",
                String.class))
        .isEqualTo("utf8mb4_unicode_ci");
    assertThat(
            jdbc.queryForObject(
                "SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='vega_private_session_v1' AND COLUMN_NAME='prototype_version'",
                String.class))
        .isEqualTo("utf8mb4_general_ci");
    var pending =
        executions.pending(
            Instant.now(), List.of("AGENT_VALIDATION", "QA_INTERNAL"), PageRequest.of(0, 100));
    assertThat(pending)
        .extracting(VegaAdjustmentExecution::getId)
        .containsExactly(91961L, 91962L, 91968L);
    assertThat(executions.pending(Instant.now(), List.of("AGENT_VALIDATION"), PageRequest.of(0, 1)))
        .extracting(VegaAdjustmentExecution::getId)
        .containsExactly(91961L);
  }

  /** Preserva a modalidade antes válida sem entregar sessões sintéticas ao provedor. */
  @Test
  void providerModeOnlyReceivesItsLegacyOrigin() {
    assertThat(executions.pending(Instant.now(), List.of("HUMAN"), PageRequest.of(0, 100)))
        .extracting(VegaAdjustmentExecution::getId)
        .containsExactly(91967L);
  }
}
