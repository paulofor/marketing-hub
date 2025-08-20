package com.marketinghub.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NicheHypothesisScheduler {
    private static final Logger log = LoggerFactory.getLogger(NicheHypothesisScheduler.class);
    private final NicheHypothesisService service;

    public NicheHypothesisScheduler(NicheHypothesisService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void run() {
        log.info("NicheHypothesisScheduler started");
        try {
            service.generateHypothesesForNiches();
        } finally {
            log.info("NicheHypothesisScheduler finished");
        }
    }
}

