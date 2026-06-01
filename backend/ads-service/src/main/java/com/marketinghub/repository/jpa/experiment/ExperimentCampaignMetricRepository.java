package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

/**
 * Repositório JPA responsável pela persistência de ExperimentCampaignMetric.
 */
public interface ExperimentCampaignMetricRepository extends JpaRepository<ExperimentCampaignMetric, Long> {
    Optional<ExperimentCampaignMetric> findByCampaign(FacebookAdsCampaign campaign);

    Optional<ExperimentCampaignMetric> findByExperiment(Experiment experiment);

    @Modifying
    @Query("delete from ExperimentCampaignMetric m where m.campaign.id in :campaignIds")
    void deleteByCampaignIds(@Param("campaignIds") Collection<String> campaignIds);
}
