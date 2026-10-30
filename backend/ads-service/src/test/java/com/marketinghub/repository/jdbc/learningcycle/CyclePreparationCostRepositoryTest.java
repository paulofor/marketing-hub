package com.marketinghub.repository.jdbc.learningcycle;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Responsabilidade: comprovar agregação real sem misturar produtos, estimativas e ausência de
 * custo.
 */
class CyclePreparationCostRepositoryTest {
  /**
   * Executa SQL real em banco local com tentativas cobradas, trabalho sem custo e outro produto.
   */
  @Test
  void aggregatesOnlyLinkedTasksAndKeepsUnknownSeparate() {
    var db =
        new EmbeddedDatabaseBuilder()
            .generateUniqueName(true)
            .setType(EmbeddedDatabaseType.H2)
            .build();
    try {
      var jdbc = new JdbcTemplate(db);
      jdbc.execute("CREATE TABLE experiment (id BIGINT, product_id BIGINT)");
      jdbc.execute(
          "CREATE TABLE agent_task (source_reference VARCHAR(255), estimated_cost_usd DECIMAL(18,8), execution_mode VARCHAR(40))");
      jdbc.execute("INSERT INTO experiment VALUES (112,7),(110,7),(111,4)");
      jdbc.execute(
          "INSERT INTO agent_task VALUES ('experiment:112',1.50,'MODEL'),('experiment:112',2.00,'MODEL'),('experiment:110',NULL,NULL),('experiment:110',NULL,'NOT_STARTED'),('experiment:112',NULL,'DETERMINISTIC'),('experiment:111',99,'MODEL'),('experiment:112@foreign',77,'MODEL')");
      var rows = new CyclePreparationCostRepository(jdbc).summarize(7L);
      assertThat(rows).hasSize(2);
      assertThat(rows.getFirst().knownUsd()).isNull();
      assertThat(rows.getFirst().unreportedTasks()).isEqualTo(1);
      assertThat(rows.getLast().knownUsd()).isEqualByComparingTo("3.50");
      assertThat(rows.getLast().unreportedTasks()).isZero();
    } finally {
      db.shutdown();
    }
  }
}
