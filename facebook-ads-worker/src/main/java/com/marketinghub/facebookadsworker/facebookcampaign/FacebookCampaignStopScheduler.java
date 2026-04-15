package com.marketinghub.facebookadsworker.facebookcampaign;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FacebookCampaignStopScheduler {

    private final FacebookCampaignService service;

    public FacebookCampaignStopScheduler(FacebookCampaignService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${facebookcampaign.stop.scheduler.delay:120000}")
    public void schedule() {
        service.pauseCampaignsRequestedForStop();
    }
}
