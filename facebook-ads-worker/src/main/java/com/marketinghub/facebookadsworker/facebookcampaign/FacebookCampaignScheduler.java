package com.marketinghub.facebookadsworker.facebookcampaign;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FacebookCampaignScheduler {
    private final FacebookCampaignService service;

    public FacebookCampaignScheduler(FacebookCampaignService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${facebookcampaign.scheduler.delay:60000}")
    public void schedule() {
        service.createCampaignsFromExperiments();
    }
}
