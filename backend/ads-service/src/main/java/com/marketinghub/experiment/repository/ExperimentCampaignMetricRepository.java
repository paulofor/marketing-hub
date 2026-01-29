package com.marketinghub.experiment.repository;

import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExperimentCampaignMetricRepository extends JpaRepository<ExperimentCampaignMetric, Long> {
    Optional<ExperimentCampaignMetric> findByCampaign(FacebookAdsCampaign campaign);
}
