package com.marketinghub.worker.deliverable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that periodically triggers deliverable generation for experiments.
 */
@Component
public class ExperimentDeliverableScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExperimentDeliverableScheduler.class);
    private final ExperimentDeliverableService service;

    public ExperimentDeliverableScheduler(ExperimentDeliverableService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        log.info("ExperimentDeliverableScheduler started");
        try {
            service.generate();
        } finally {
            log.info("ExperimentDeliverableScheduler finished");
        }
    }
}
