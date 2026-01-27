package com.marketinghub.worker.adset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that periodically generates ad sets from approved targeting elements.
 */
@Component
public class AudienceAdSetScheduler {
    private static final Logger log = LoggerFactory.getLogger(AudienceAdSetScheduler.class);
    private final AudienceAdSetService service;

    public AudienceAdSetScheduler(AudienceAdSetService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        log.info("AudienceAdSetScheduler started");
        try {
            service.generate();
        } finally {
            log.info("AudienceAdSetScheduler finished");
        }
    }
}
