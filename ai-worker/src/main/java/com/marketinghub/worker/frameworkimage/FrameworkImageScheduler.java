package com.marketinghub.worker.frameworkimage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FrameworkImageScheduler {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageScheduler.class);

    private final FrameworkImageService service;

    public FrameworkImageScheduler(FrameworkImageService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        long startedAt = System.currentTimeMillis();
        try {
            service.processPending();
        } catch (Exception ex) {
            log.error("FrameworkImageScheduler cycle failed", ex);
            throw ex;
        } finally {
            log.info("FrameworkImageScheduler finished (elapsedMs={})", System.currentTimeMillis() - startedAt);
        }
    }
}
