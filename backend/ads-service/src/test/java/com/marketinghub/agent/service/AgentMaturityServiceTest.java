package com.marketinghub.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.agent.Agent;
import com.marketinghub.agent.dto.AgentMaturityDto;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Responsabilidade: proteger a consolidação de maturidade dos agentes governados. */
class AgentMaturityServiceTest {

  /** Comprova que revisões iterativas do Aprovador Meta entram no ciclo compartilhado. */
  @Test
  void summarizesMetaAdApproverReviewsAttemptsCostsAndResults() {
    AgentService agentService = mock(AgentService.class);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    Agent agent = new Agent();
    agent.setId(6L);
    agent.setAgentKey("meta-ad-approver");
    agent.setName("Aprovador de Anúncios Meta");
    when(agentService.list()).thenReturn(List.of(agent));
    when(jdbc.queryForMap(contains("FROM creative WHERE agent_reviewed_at IS NOT NULL")))
        .thenReturn(
            Map.of(
                "total", 5L,
                "completed", 4L,
                "failed", 1L,
                "cost", new BigDecimal("1.2700"),
                "last_at", Timestamp.from(Instant.parse("2026-08-09T10:00:00Z"))));
    when(jdbc.queryForMap(contains("confirmed_count")))
        .thenReturn(Map.of("open_count", 2L, "resolved_count", 5L, "confirmed_count", 3L));

    AgentMaturityDto maturity = new AgentMaturityService(agentService, jdbc).list().getFirst();

    assertThat(maturity.executions()).isEqualTo(5);
    assertThat(maturity.completedExecutions()).isEqualTo(4);
    assertThat(maturity.failedExecutions()).isEqualTo(1);
    assertThat(maturity.openTasks()).isEqualTo(2);
    assertThat(maturity.resolvedTasks()).isEqualTo(5);
    assertThat(maturity.confirmedResults()).isEqualTo(3);
    assertThat(maturity.estimatedCost()).isEqualByComparingTo("1.2700");
    assertThat(maturity.maturityLevel()).isEqualTo("EM_VALIDACAO");
    assertThat(maturity.lastExecutionAt()).isEqualTo(Instant.parse("2026-08-09T10:00:00Z"));
  }
}
