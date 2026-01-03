package com.marketinghub.facebookadsworker.facebookpixel;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FacebookPixelScheduler {

    private final FacebookPixelService service;

    public FacebookPixelScheduler(FacebookPixelService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${facebookpixel.scheduler.delay:60000}")
    public void schedule() {
        service.syncPixelsAndConversions();
    }
}
