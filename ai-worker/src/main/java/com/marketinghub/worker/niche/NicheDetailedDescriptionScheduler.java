package com.marketinghub.worker.niche;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NicheDetailedDescriptionScheduler {
    private static final Logger log = LoggerFactory.getLogger(NicheDetailedDescriptionScheduler.class);
    private final NicheDetailedDescriptionGenerationService service;

    public NicheDetailedDescriptionScheduler(NicheDetailedDescriptionGenerationService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        log.info("NicheDetailedDescriptionScheduler started");
        try {
            service.generate();
        } finally {
            log.info("NicheDetailedDescriptionScheduler finished");
        }
    }
}
