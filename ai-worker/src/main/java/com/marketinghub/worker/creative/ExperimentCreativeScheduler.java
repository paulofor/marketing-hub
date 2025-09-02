package com.marketinghub.worker.creative;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that periodically triggers creative generation for experiments.
 */
@Component
public class ExperimentCreativeScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExperimentCreativeScheduler.class);
    private final ExperimentCreativeService service;

    public ExperimentCreativeScheduler(ExperimentCreativeService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        log.info("ExperimentCreativeScheduler started");
        try {
            service.generate();
        } finally {
            log.info("ExperimentCreativeScheduler finished");
        }
    }
}
