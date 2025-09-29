package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FacebookTokenRenewalScheduler {
    private final FacebookTokenRenewalService renewalService;

    public FacebookTokenRenewalScheduler(FacebookTokenRenewalService renewalService) {
        this.renewalService = renewalService;
    }

    @Scheduled(fixedDelayString = "${facebook.token-renewal.scheduler.delay:21600000}")
    public void scheduleRenewal() {
        renewalService.renewTokensIfNeeded();
    }
}
