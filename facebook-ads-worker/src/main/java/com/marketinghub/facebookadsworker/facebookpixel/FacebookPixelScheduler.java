package com.marketinghub.facebookadsworker.facebookpixel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "facebookpixel.enabled", havingValue = "true")
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
