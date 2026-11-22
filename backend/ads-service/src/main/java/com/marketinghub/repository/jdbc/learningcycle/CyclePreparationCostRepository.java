package com.marketinghub.repository.jdbc.learningcycle;

import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.CostWaiver;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.CycleFinancialReview.CostSlice;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Responsabilidade: consultar cobertura e dispensas de custos de tarefas por produto. */
@Repository
@RequiredArgsConstructor
public class CyclePreparationCostRepository {
  private final JdbcTemplate jdbc;

  /** Inclui tentativas canceladas cobradas e mantém custos ausentes separados de zero. */
  public List<CostSlice> summarize(Long productId) {
    return jdbc.query(
        """
        SELECT e.id AS experiment_id, SUM(t.estimated_cost_usd) AS known_usd,
          SUM(CASE WHEN t.estimated_cost_usd IS NULL
            AND (t.execution_mode IS NULL OR t.execution_mode NOT IN ('DETERMINISTIC', 'NOT_STARTED'))
            THEN 1 ELSE 0 END) AS unreported
        FROM experiment e JOIN agent_task t ON t.source_reference = CONCAT('experiment:', e.id)
        WHERE e.product_id = ?
        GROUP BY e.id ORDER BY e.id
        """,
        (rs, row) ->
            new CostSlice(
                rs.getLong("experiment_id"),
                rs.getBigDecimal("known_usd"),
                rs.getInt("unreported")),
        productId);
  }

  /** Lista custos ausentes e dispensas históricas sem alterar valores ou duplicar tarefas. */
  public List<CostWaiver.Task> waiverTasks(Long productId) {
    return jdbc.query(
        """
      SELECT t.id, e.id experiment_id, t.status, t.estimated_cost_usd,
        EXISTS (SELECT 1 FROM learning_sales_cycle_event_v1 ev
          JOIN learning_sales_cycle_v1 c ON c.id=ev.cycle_id
          WHERE c.product_id=e.product_id AND ev.action='WAIVE_LEGACY_AGENT_COST'
          AND JSON_CONTAINS(ev.evidence_json, CAST(t.id AS JSON), '$.taskIds')) waived
      FROM experiment e JOIN agent_task t ON t.source_reference=CONCAT('experiment:',e.id)
      WHERE e.product_id=?
        AND (t.estimated_cost_usd IS NULL
          AND (t.execution_mode IS NULL OR t.execution_mode NOT IN ('DETERMINISTIC','NOT_STARTED'))
          OR EXISTS (SELECT 1 FROM learning_sales_cycle_event_v1 ev
            JOIN learning_sales_cycle_v1 c ON c.id=ev.cycle_id
            WHERE c.product_id=e.product_id AND ev.action='WAIVE_LEGACY_AGENT_COST'
            AND JSON_CONTAINS(ev.evidence_json, CAST(t.id AS JSON), '$.taskIds')))
      ORDER BY t.id
      """,
        (rs, row) -> {
          boolean waived = rs.getBoolean("waived");
          var cost = rs.getBigDecimal("estimated_cost_usd");
          String status = rs.getString("status");
          return new CostWaiver.Task(
              rs.getLong("id"),
              rs.getLong("experiment_id"),
              status,
              cost,
              waived,
              cost == null
                  && !waived
                  && java.util.Set.of("CANCELLED", "BLOCKED", "FAILED", "COMPLETED")
                      .contains(status));
        },
        productId);
  }

  /**
   * Conta somente ausências ainda bloqueantes; dispensa não modifica o demonstrativo financeiro.
   */
  public long blockingTasks(Long productId) {
    return waiverTasks(productId).stream().filter(t -> t.costUsd() == null && !t.waived()).count();
  }
}
