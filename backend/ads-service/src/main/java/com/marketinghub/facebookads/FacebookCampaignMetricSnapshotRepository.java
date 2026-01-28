package com.marketinghub.facebookads;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacebookCampaignMetricSnapshotRepository extends JpaRepository<FacebookCampaignMetricSnapshot, Long> {

    @EntityGraph(attributePaths = {"experiment", "campaign"})
    List<FacebookCampaignMetricSnapshot> findTop30ByExperimentIdOrderByCapturedAtDesc(Long experimentId);

    @EntityGraph(attributePaths = {"experiment", "campaign"})
    @Query("""
        select snapshot from FacebookCampaignMetricSnapshot snapshot
        where snapshot.id in (
            select max(innerSnapshot.id) from FacebookCampaignMetricSnapshot innerSnapshot
            group by innerSnapshot.experiment.id
        )
        order by snapshot.capturedAt desc
        """)
    List<FacebookCampaignMetricSnapshot> findLatestPerExperiment();

    @EntityGraph(attributePaths = {"experiment", "campaign"})
    @Query("""
        select snapshot from FacebookCampaignMetricSnapshot snapshot
        where snapshot.campaign.id = :campaignId
        order by snapshot.capturedAt desc
        """)
    List<FacebookCampaignMetricSnapshot> findAllByCampaignIdOrderByCapturedAtDesc(@Param("campaignId") String campaignId);
}
