package com.marketinghub.facebookadsworker.facebookcampaign;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FacebookCampaignMetricsScheduler {
    private final FacebookCampaignService campaignService;

    public FacebookCampaignMetricsScheduler(FacebookCampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @Scheduled(fixedDelayString = "${facebookcampaign.metrics.scheduler.delay:300000}")
    public void schedule() {
        campaignService.syncRunningCampaignMetrics();
    }
}
