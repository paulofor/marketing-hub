package com.marketinghub.experiment.funnel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Repositório de eventos do funil do experimento.
 */
public interface ExperimentFunnelEventRepository extends JpaRepository<ExperimentFunnelEvent, Long> {

    @Query("""
            select e.stage as stage,
                   count(e.id) as total,
                   count(distinct e.lead) as uniqueLeads,
                   max(e.occurredAt) as lastEvent
            from ExperimentFunnelEvent e
            where e.experiment.id = :experimentId
            group by e.stage
            """)
    List<StageAggregation> aggregateByExperiment(@Param("experimentId") Long experimentId);

    interface StageAggregation {
        ExperimentFunnelStage getStage();
        long getTotal();
        Long getUniqueLeads();
        Instant getLastEvent();
    }
}
