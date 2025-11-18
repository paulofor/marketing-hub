package com.marketinghub.facebookadsworker.facebookinterest;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FacebookInterestValidationScheduler {
    private final FacebookInterestValidationService validationService;

    public FacebookInterestValidationScheduler(FacebookInterestValidationService validationService) {
        this.validationService = validationService;
    }

    @Scheduled(fixedDelayString = "${facebook.interest-validation.scheduler.delay:900000}")
    public void scheduleValidation() {
        validationService.validatePendingInterests();
    }
}
