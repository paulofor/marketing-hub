package com.marketinghub.facebookadsworker.instagramcampaign;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InstagramCampaignScheduler {
    private final InstagramCampaignService service;

    public InstagramCampaignScheduler(InstagramCampaignService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${instagramcampaign.scheduler.delay:60000}")
    public void schedule() {
        service.createCampaignsFromAuthorizedCreatives();
    }
}

