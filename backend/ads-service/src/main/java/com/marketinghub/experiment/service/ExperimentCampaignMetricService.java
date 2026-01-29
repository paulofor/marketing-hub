package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.repository.ExperimentCampaignMetricRepository;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class ExperimentCampaignMetricService {
    private final ExperimentCampaignMetricRepository repository;
    private final FacebookAdsCampaignRepository campaignRepository;

    public ExperimentCampaignMetricService(ExperimentCampaignMetricRepository repository,
                                           FacebookAdsCampaignRepository campaignRepository) {
        this.repository = repository;
        this.campaignRepository = campaignRepository;
    }

    @Transactional
    public ExperimentCampaignMetric upsert(String campaignId,
                                           LocalDate dateStart,
                                           LocalDate dateStop,
                                           Long impressions,
                                            Long clicks,
                                            Long leads,
                                           BigDecimal spend) {
        FacebookAdsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Facebook campaign not found: " + campaignId));
        Experiment experiment = campaign.getExperiment();
        BigDecimal normalizedSpend = spend != null ? spend.setScale(2, RoundingMode.HALF_UP) : null;
        ExperimentCampaignMetric metric = repository.findByCampaign(campaign)
                .orElseGet(() -> ExperimentCampaignMetric.builder()
                        .campaign(campaign)
                        .experiment(experiment)
                        .build());
        metric.setDateStart(dateStart);
        metric.setDateStop(dateStop);
        metric.setImpressions(impressions);
        metric.setClicks(clicks);
        metric.setLeads(leads);
        metric.setSpend(normalizedSpend);
        metric.setCpc(calculateCpc(normalizedSpend, clicks));
        metric.setCpl(calculateCpl(normalizedSpend, leads));
        ExperimentCampaignMetric saved = repository.save(metric);
        updateExperimentTotalCost(experiment, normalizedSpend);
        return saved;
    }

    private BigDecimal calculateCpc(BigDecimal spend, Long clicks) {
        if (spend == null || clicks == null || clicks == 0) {
            return BigDecimal.ZERO;
        }
        return spend.divide(BigDecimal.valueOf(clicks), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCpl(BigDecimal spend, Long leads) {
        if (spend == null || leads == null || leads == 0) {
            return BigDecimal.ZERO;
        }
        return spend.divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP);
    }

    private void updateExperimentTotalCost(Experiment experiment, BigDecimal spend) {
        if (experiment == null || spend == null) {
            return;
        }
        experiment.setTotalCost(spend);
    }
}
