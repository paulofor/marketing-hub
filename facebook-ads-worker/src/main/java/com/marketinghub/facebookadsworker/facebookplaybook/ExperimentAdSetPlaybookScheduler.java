package com.marketinghub.facebookadsworker.facebookplaybook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically processes Facebook jobs from the ad set playbook.
 */
@Component
public class ExperimentAdSetPlaybookScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentAdSetPlaybookScheduler.class);

    private final ExperimentAdSetPlaybookService service;

    public ExperimentAdSetPlaybookScheduler(ExperimentAdSetPlaybookService service) {
        this.service = service;
    }

    @Scheduled(cron = "30 */2 * * * *")
    public void run() {
        LOGGER.debug("Executando scheduler do playbook no worker do Facebook");
        service.processQueue();
    }
}
