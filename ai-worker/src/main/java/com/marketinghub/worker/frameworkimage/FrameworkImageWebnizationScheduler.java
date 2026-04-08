package com.marketinghub.worker.frameworkimage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FrameworkImageWebnizationScheduler {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageWebnizationScheduler.class);

    private final FrameworkImageWebnizationService service;

    public FrameworkImageWebnizationScheduler(FrameworkImageWebnizationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${framework-image.webnization.scheduler.fixed-delay-ms:20000}")
    public void run() {
        long startedAt = System.currentTimeMillis();
        try {
            service.processPending();
        } catch (Exception ex) {
            log.error("FrameworkImageWebnizationScheduler cycle failed", ex);
        } finally {
            log.info("FrameworkImageWebnizationScheduler finished (elapsedMs={})", System.currentTimeMillis() - startedAt);
        }
    }
}
