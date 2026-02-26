package com.marketinghub.facebookadsworker.facebooktargeting.metaads;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MetaAdsTargetingEnrichmentScheduler {
    private final MetaAdsTargetingEnrichmentService service;

    public MetaAdsTargetingEnrichmentScheduler(MetaAdsTargetingEnrichmentService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${facebook.targeting.metaads.scheduler.delay:300000}")
    public void process() {
        service.processPendingElements();
    }
}
