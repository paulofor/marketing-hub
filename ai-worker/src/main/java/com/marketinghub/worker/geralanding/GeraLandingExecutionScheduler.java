package com.marketinghub.worker.geralanding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GeraLandingExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingExecutionScheduler.class);

    private final GeraLandingExecutionService service;

    public GeraLandingExecutionScheduler(GeraLandingExecutionService service) {
        this.service = service;
    }

    @Scheduled(cron = "${geralanding.execution.fixed-cron:0 */1 * * * *}")
    public void run() {
        long startedAt = System.currentTimeMillis();
        try {
            service.processPendingExecutions();
        } catch (Exception ex) {
            log.error("GeraLandingExecutionScheduler cycle failed", ex);
            throw ex;
        } finally {
            log.info("GeraLandingExecutionScheduler finished (elapsedMs={})", System.currentTimeMillis() - startedAt);
        }
    }
}
