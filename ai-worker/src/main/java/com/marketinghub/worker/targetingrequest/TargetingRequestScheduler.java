package com.marketinghub.worker.targetingrequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TargetingRequestScheduler {
    private static final Logger log = LoggerFactory.getLogger(TargetingRequestScheduler.class);

    private final TargetingRequestGenerationService service;

    public TargetingRequestScheduler(TargetingRequestGenerationService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */2 * * * *")
    public void run() {
        log.info("TargetingRequestScheduler started");
        try {
            service.processPending();
        } finally {
            log.info("TargetingRequestScheduler finished");
        }
    }
}
