package com.marketinghub.worker.adset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that periodically generates ad sets from approved targeting elements.
 */
@Component
public class TargetingAdSetScheduler {
    private static final Logger log = LoggerFactory.getLogger(TargetingAdSetScheduler.class);
    private final TargetingAdSetService service;

    public TargetingAdSetScheduler(TargetingAdSetService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        log.info("TargetingAdSetScheduler started");
        try {
            service.generate();
        } finally {
            log.info("TargetingAdSetScheduler finished");
        }
    }
}
