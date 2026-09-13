package com.marketinghub.repository.jpa.salesvideo;

import static org.assertj.core.api.Assertions.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Responsabilidade: comprovar no SQL a segregação e precedência da tentativa audiovisual atual. */
class VideoProductionCycleGuidanceQueryTest {
  private JdbcTemplate jdbc;
  private NamedParameterJdbcTemplate named;
  private String query;
  private final Instant changedAt = Instant.parse("2026-09-12T08:00:00Z");

  /** Cria tabelas isoladas com as colunas canônicas usadas pela consulta de orientação. */
  @BeforeEach
  void setup() throws NoSuchMethodException {
    var source =
        new DriverManagerDataSource(
            "jdbc:h2:mem:"
                + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
            "sa",
            "");
    jdbc = new JdbcTemplate(source);
    named = new NamedParameterJdbcTemplate(source);
    jdbc.execute(
        "CREATE TABLE video_project (id BIGINT PRIMARY KEY, product_id BIGINT, experiment_id BIGINT, campaign_key VARCHAR(191), strategy_role VARCHAR(64))");
    jdbc.execute(
        "CREATE TABLE video_production_cycle (id BIGINT PRIMARY KEY, video_project_id BIGINT, product_id BIGINT, experiment_id BIGINT, created_at DATETIME, status VARCHAR(40))");
    jdbc.update("INSERT INTO video_project VALUES (4,4,92,'musa-v12','CAMPAIGN_QUALIFICATION')");
    jdbc.update(
        "INSERT INTO video_production_cycle VALUES (12,4,4,92,?,'PROVIDER_PREFLIGHT_ONLY_BLOCKED')",
        Timestamp.from(changedAt.plusSeconds(1)));
    query =
        VideoProductionCycleRepository.class
            .getMethod(
                "findLatestForLearningCycle",
                Long.class,
                Long.class,
                String.class,
                String.class,
                Instant.class)
            .getAnnotation(Query.class)
            .value();
  }

  /** Libera a base em memória mesmo se uma asserção falhar. */
  @AfterEach
  void cleanup() {
    jdbc.execute("SHUTDOWN");
  }

  /** Executa a consulta versionada com os parâmetros da ocorrência sintética. */
  private List<Map<String, Object>> results() {
    return named.queryForList(
        query,
        Map.of(
            "productId",
            4L,
            "experimentId",
            92L,
            "productVersion",
            "musa-v12",
            "strategyRole",
            "CAMPAIGN_QUALIFICATION",
            "versionChangedAt",
            Timestamp.from(changedAt)));
  }

  /** Seleciona a tentativa correspondente sem usar outra peça ou época. */
  @Test
  void selectsExactVersion() {
    assertThat(results())
        .singleElement()
        .satisfies(row -> assertThat(row.get("id")).isEqualTo(12L));
  }

  /** Nunca resgata um bloqueio antigo quando uma nova tentativa já foi aberta. */
  @Test
  void latestAttemptWinsEvenWhenItIsNotBlocked() {
    jdbc.update(
        "INSERT INTO video_production_cycle VALUES (13,4,4,92,?,'PENDING_PROVIDER_PREFLIGHT_ONLY')",
        Timestamp.from(changedAt.plusSeconds(2)));
    assertThat(results())
        .singleElement()
        .satisfies(row -> assertThat(row.get("id")).isEqualTo(13L));
  }

  /** Rejeita divergências em qualquer lado do vínculo entre projeto e produção. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "projectProduct",
        "projectExperiment",
        "cycleProduct",
        "cycleExperiment",
        "version",
        "role",
        "time"
      })
  void excludesOtherContexts(String field) {
    String update =
        switch (field) {
          case "projectProduct" -> "UPDATE video_project SET product_id=5";
          case "projectExperiment" -> "UPDATE video_project SET experiment_id=91";
          case "cycleProduct" -> "UPDATE video_production_cycle SET product_id=5";
          case "cycleExperiment" -> "UPDATE video_production_cycle SET experiment_id=91";
          case "version" -> "UPDATE video_project SET campaign_key='musa-v11'";
          case "role" -> "UPDATE video_project SET strategy_role='PDE_HERO_CONVERSION'";
          case "time" -> "UPDATE video_production_cycle SET created_at='2026-09-11 08:00:00'";
          default -> throw new AssertionError(field);
        };
    jdbc.update(update);
    assertThat(results()).isEmpty();
  }
}
