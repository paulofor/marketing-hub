package com.marketinghub.repository.jpa.agentcostwaiver;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Responsabilidade: consultar recibos de dispensa por produto sem ampliar o repositório genérico de
 * eventos.
 */
public interface AgentCostWaiverEventRepository extends Repository<LearningSalesCycleEvent, Long> {
  /** Preserva decisões de todos os ciclos do produto, sem aplicar a outros produtos. */
  @Query(
      "select e from LearningSalesCycleEvent e where e.action = 'WAIVE_LEGACY_AGENT_COST' and e.cycleId in (select c.id from LearningSalesCycle c where c.productId = :productId) order by e.id desc")
  List<LearningSalesCycleEvent> findByProductId(@Param("productId") Long productId);
}
