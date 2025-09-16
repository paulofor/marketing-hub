package com.marketinghub.worker.audience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler que dispara a geração automática de públicos.
 */
@Component
public class NicheAudienceScheduler {
    private static final Logger log = LoggerFactory.getLogger(NicheAudienceScheduler.class);
    private final NicheAudienceService service;

    public NicheAudienceScheduler(NicheAudienceService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        log.info("NicheAudienceScheduler started");
        try {
            service.generate();
        } finally {
            log.info("NicheAudienceScheduler finished");
        }
    }
}
