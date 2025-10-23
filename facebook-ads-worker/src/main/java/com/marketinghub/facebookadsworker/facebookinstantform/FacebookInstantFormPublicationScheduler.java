package com.marketinghub.facebookadsworker.facebookinstantform;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FacebookInstantFormPublicationScheduler {
    private final FacebookInstantFormPublicationService service;

    public FacebookInstantFormPublicationScheduler(FacebookInstantFormPublicationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${facebookinstantform.scheduler.delay:60000}")
    public void schedule() {
        service.processApprovedInstantFormDrafts();
    }
}
