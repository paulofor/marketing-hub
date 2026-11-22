package com.marketinghub.repository.jdbc.learningcycle;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Homologa a consulta real em MySQL 5.7 isolado, sem dados ou credenciais produtivos. */
@EnabledIfEnvironmentVariable(
    named = "WAIVER_TEST_MYSQL_URL",
    matches = "jdbc:mysql://(127\\.0\\.0\\.1|sandbox-docker):[0-9]+/waiver_test.*")
class CostWaiverMysqlTest {
  /** Prova persistência, isolamento, nova pendência e recuperação sem multiplicar custos. */
  @Test
  void preservesUnknownsAndCountsRecoveredCostOnce() {
    var jdbc =
        new JdbcTemplate(
            new DriverManagerDataSource(
                System.getenv("WAIVER_TEST_MYSQL_URL"),
                "root",
                System.getenv().getOrDefault("WAIVER_TEST_MYSQL_PASSWORD", "")));
    jdbc.execute(
        "DROP TABLE IF EXISTS agent_task, experiment, learning_sales_cycle_v1, learning_sales_cycle_event_v1");
    jdbc.execute("CREATE TABLE experiment(id BIGINT PRIMARY KEY, product_id BIGINT)");
    jdbc.execute(
        "CREATE TABLE agent_task(id BIGINT PRIMARY KEY, source_reference VARCHAR(80), status VARCHAR(32), execution_mode VARCHAR(32), estimated_cost_usd DECIMAL(20,8))");
    jdbc.execute("CREATE TABLE learning_sales_cycle_v1(id BIGINT PRIMARY KEY, product_id BIGINT)");
    jdbc.execute(
        "CREATE TABLE learning_sales_cycle_event_v1(id BIGINT PRIMARY KEY,cycle_id BIGINT, action VARCHAR(32),evidence_json LONGTEXT)");
    jdbc.update("INSERT INTO experiment VALUES (90,4),(91,9004)");
    jdbc.update("INSERT INTO learning_sales_cycle_v1 VALUES (22,4),(23,9004)");
    jdbc.update(
        "INSERT INTO agent_task VALUES (246,'experiment:90','CANCELLED',NULL,NULL),(247,'experiment:90','BLOCKED','MODEL',NULL),(248,'experiment:90','COMPLETED','MODEL',113.33),(249,'experiment:91','CANCELLED',NULL,NULL),(250,'experiment:90','COMPLETED','DETERMINISTIC',NULL)");
    var repo = new CyclePreparationCostRepository(jdbc);
    assertThat(repo.blockingTasks(4L)).isEqualTo(2);
    jdbc.update(
        "INSERT INTO learning_sales_cycle_event_v1 VALUES (1,22,'WAIVE_LEGACY_AGENT_COST','{\"taskIds\":[246]}')");
    assertThat(new CyclePreparationCostRepository(jdbc).blockingTasks(4L)).isEqualTo(1);
    assertThat(repo.blockingTasks(9004L)).isEqualTo(1);
    assertThat(repo.waivedUnknownTasks(4L)).isEqualTo(1);
    assertThat(repo.waivedUnknownTasks(9004L)).isZero();
    assertThat(repo.summarize(4L).getFirst().knownUsd()).isEqualByComparingTo("113.33");
    assertThat(repo.summarize(4L).getFirst().unreportedTasks()).isEqualTo(2);
    assertThat(repo.waiverTasks(4L).getFirst().costUsd()).isNull();
    jdbc.update(
        "INSERT INTO learning_sales_cycle_event_v1 VALUES (2,22,'WAIVE_LEGACY_AGENT_COST','{\"taskIds\":[246]}')");
    assertThat(repo.waiverTasks(4L)).hasSize(2);
    jdbc.update("UPDATE agent_task SET execution_mode='DETERMINISTIC' WHERE id=246");
    assertThat(repo.waivedUnknownTasks(4L)).isZero();
    assertThat(repo.summarize(4L).getFirst().unreportedTasks()).isEqualTo(1);
    jdbc.update("UPDATE agent_task SET execution_mode='MODEL' WHERE id=246");
    jdbc.update("INSERT INTO agent_task VALUES (251,'experiment:90','CANCELLED',NULL,NULL)");
    assertThat(repo.blockingTasks(4L)).isEqualTo(2);
    jdbc.update("UPDATE agent_task SET estimated_cost_usd=12.50 WHERE id=246");
    assertThat(repo.summarize(4L).getFirst().knownUsd()).isEqualByComparingTo("125.83");
    assertThat(repo.summarize(4L).getFirst().unreportedTasks()).isEqualTo(2);
    assertThat(repo.waiverTasks(4L).getFirst().costUsd()).isEqualByComparingTo("12.50");
    assertThat(repo.waivedUnknownTasks(4L)).isZero();
  }
}
