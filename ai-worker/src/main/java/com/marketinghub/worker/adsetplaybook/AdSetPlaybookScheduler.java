package com.marketinghub.worker.adsetplaybook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically triggers the AI processing for the ad set playbook.
 */
@Component
public class AdSetPlaybookScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdSetPlaybookScheduler.class);

    private final AdSetPlaybookService service;

    public AdSetPlaybookScheduler(AdSetPlaybookService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */3 * * * *")
    public void run() {
        LOGGER.debug("Executando scheduler do roteiro de ad sets");
        service.processQueue();
    }
}
