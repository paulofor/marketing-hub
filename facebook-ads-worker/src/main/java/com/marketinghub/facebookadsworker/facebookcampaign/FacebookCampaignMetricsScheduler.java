package com.marketinghub.facebookadsworker.facebookcampaign;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FacebookCampaignMetricsScheduler {
    private final FacebookCampaignMetricsService service;

    public FacebookCampaignMetricsScheduler(FacebookCampaignMetricsService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${facebookcampaign.metrics.scheduler.delay:300000}")
    public void scheduleMetricsSync() {
        service.syncCampaignMetrics();
    }
}
