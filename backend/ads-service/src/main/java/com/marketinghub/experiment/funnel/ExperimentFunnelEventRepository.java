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

    String RENDER_COMPLETE_SOURCE = "lead-portal-render-complete";
    String SUBMISSION_SOURCE = "lead_portal_submission";

    @Query("""
            select e.stage as stage,
                   count(e.id) as total,
                   count(distinct e.lead) as uniqueLeads,
                   max(e.occurredAt) as lastEvent
            from ExperimentFunnelEvent e
            where e.experiment.id = :experimentId
              and (e.source is null or e.source <> :excludedSource)
            group by e.stage
            """)
    List<StageAggregation> aggregateByExperiment(@Param("experimentId") Long experimentId,
                                                 @Param("excludedSource") String excludedSource);

    interface StageAggregation {
        ExperimentFunnelStage getStage();
        long getTotal();
        Long getUniqueLeads();
        Instant getLastEvent();
    }
}
