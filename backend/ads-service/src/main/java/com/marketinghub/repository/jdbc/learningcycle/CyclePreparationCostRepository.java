package com.marketinghub.repository.jdbc.learningcycle;

import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.CycleFinancialReview.CostSlice;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Responsabilidade: agregar estimativas de tarefas do produto sem ler prompts ou respostas. */
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
}
