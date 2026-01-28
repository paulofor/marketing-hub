package com.marketinghub.worker.targeting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TargetingElementScheduler {
    private static final Logger log = LoggerFactory.getLogger(TargetingElementScheduler.class);
    private final TargetingElementGenerationService service;

    public TargetingElementScheduler(TargetingElementGenerationService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        log.info("TargetingElementScheduler started");
        try {
            service.generate();
        } finally {
            log.info("TargetingElementScheduler finished");
        }
    }
}

