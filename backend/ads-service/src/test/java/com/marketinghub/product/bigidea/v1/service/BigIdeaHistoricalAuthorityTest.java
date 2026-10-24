package com.marketinghub.product.bigidea.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.product.bigidea.v1.ProductBigIdea;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reproduz direção retroativa em Mira, outra identidade e estratégia inicial legítima. */
class BigIdeaHistoricalAuthorityTest {
  /** Preserva ausência anterior à pesquisa sem apagar a revisão atual ou tarefas históricas. */
  @ParameterizedTest
  @ValueSource(longs = {10, 93010})
  void preservesHistoricalAbsence(long productId) {
    var tasks = mock(AgentTaskRepository.class);
    var history = new BigIdeaHistoricalAuthority(tasks, new ObjectMapper());
    var first = mock(AgentTaskRepository.BigIdeaHistory.class);
    var idea = new ProductBigIdea();
    idea.setProductId(productId);
    idea.setId(3L);
    when(tasks.findFirstBigIdeaHistory("experiment:102")).thenReturn(Optional.of(first));
    assertThat(history.preservesAbsence(idea, "experiment:102")).isTrue();
    when(first.getProductBigIdeaJson()).thenReturn("{\"status\":\"MISSING\"}");
    assertThat(history.preservesAbsence(idea, "experiment:102")).isTrue();
    when(first.getProductBigIdeaJson()).thenReturn("{\"status\":\"DEFINED\"}");
    assertThat(history.preservesAbsence(idea, "experiment:102")).isFalse();
    assertThat(history.preservesAbsence(idea, "experiment:103")).isFalse();
    assertThat(idea.getId()).isEqualTo(3);
  }

  /** Criação inicial de Atena no próprio contexto permanece válida; outro contexto não herda. */
  @Test
  void allowsExplicitInitialStrategyOnlyForItsOwnContext() {
    var tasks = mock(AgentTaskRepository.class);
    var history = new BigIdeaHistoricalAuthority(tasks, new ObjectMapper());
    when(tasks.findFirstBigIdeaHistory("experiment:102"))
        .thenReturn(Optional.of(mock(AgentTaskRepository.BigIdeaHistory.class)));
    var idea = new ProductBigIdea();
    idea.setSourceTaskId(901L);
    var strategy = new AgentTask();
    strategy.setSourceReference("experiment:102@strategy");
    strategy.setProcessActivityId("marketStrategy");
    var agent = new Agent();
    agent.setAgentKey("experiment-strategist");
    strategy.setAssignedAgent(agent);
    when(tasks.findById(901L)).thenReturn(Optional.of(strategy));
    assertThat(history.preservesAbsence(idea, "experiment:102")).isFalse();
    strategy.setSourceReference("experiment:1020");
    assertThat(history.preservesAbsence(idea, "experiment:102")).isTrue();
    strategy.setSourceReference("experiment:102");
    strategy.setProcessActivityId("customer");
    assertThat(history.preservesAbsence(idea, "experiment:102")).isTrue();
  }

  /** Fotografia ilegível bloqueia em vez de aplicar silenciosamente a ideia atual. */
  @Test
  void rejectsMalformedHistoricalSnapshot() {
    var tasks = mock(AgentTaskRepository.class);
    var first = mock(AgentTaskRepository.BigIdeaHistory.class);
    when(tasks.findFirstBigIdeaHistory("experiment:102")).thenReturn(Optional.of(first));
    when(first.getProductBigIdeaJson()).thenReturn("{invalid");
    assertThatThrownBy(
            () ->
                new BigIdeaHistoricalAuthority(tasks, new ObjectMapper())
                    .preservesAbsence(new ProductBigIdea(), "experiment:102"))
        .hasMessageContaining("direção histórica");
  }

  /** Executa SQL local com alias, ordenação e ID de prefixo comum sem ler todo o histórico. */
  @Test
  void selectsEarliestExactScopeInSql() throws Exception {
    var source =
        new org.springframework.jdbc.datasource.DriverManagerDataSource(
            "jdbc:h2:mem:idea_history_" + java.util.UUID.randomUUID() + ";MODE=MySQL", "sa", "");
    try (var connection = source.getConnection()) {
      var jdbc =
          new org.springframework.jdbc.core.JdbcTemplate(
              new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true));
      jdbc.execute(
          "CREATE TABLE agent_task(id BIGINT,source_reference VARCHAR(200),product_big_idea_json CLOB,created_at DATETIME)");
      jdbc.update(
          "INSERT INTO agent_task VALUES(1,'experiment:1020',NULL,'2026-10-01 00:00:00'),(620,'experiment:102@old',NULL,'2026-10-07 00:00:00'),(802,'experiment:102','{}','2026-10-24 00:00:00')");
      String query =
          AgentTaskRepository.class
              .getMethod("findFirstBigIdeaHistory", String.class)
              .getAnnotation(org.springframework.data.jpa.repository.Query.class)
              .value();
      var named = new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbc);
      assertThat(
              named.queryForMap(query, java.util.Map.of("reference", "experiment:102")).get("ID"))
          .isEqualTo(620L);
      assertThat(named.queryForList(query, java.util.Map.of("reference", "experiment:103")))
          .isEmpty();
    }
  }
}
