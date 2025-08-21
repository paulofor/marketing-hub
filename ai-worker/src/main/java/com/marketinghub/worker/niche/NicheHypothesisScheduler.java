package com.marketinghub.worker.niche;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that periodically triggers hypothesis generation for niches.
 */
@Component
public class NicheHypothesisScheduler {
    private static final Logger log = LoggerFactory.getLogger(NicheHypothesisScheduler.class);
    private final NicheHypothesisService service;

    public NicheHypothesisScheduler(NicheHypothesisService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        log.info("NicheHypothesisScheduler started");
        try {
            service.generate();
        } finally {
            log.info("NicheHypothesisScheduler finished");
        }
    }
}
