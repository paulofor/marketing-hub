package com.marketinghub.worker.frameworkimage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class FrameworkImageWebnizationScheduler {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageWebnizationScheduler.class);

    private final FrameworkImageWebnizationService service;
    private final boolean enabled;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public FrameworkImageWebnizationScheduler(FrameworkImageWebnizationService service,
                                              @Value("${framework-image.webnization.scheduler.enabled:true}") boolean enabled) {
        this.service = service;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${framework-image.webnization.scheduler.fixed-delay-ms:20000}")
    public void run() {
        if (!enabled) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("FrameworkImageWebnizationScheduler previous cycle still running");
            return;
        }
        long startedAt = System.currentTimeMillis();
        try {
            service.processPending();
        } catch (Exception ex) {
            log.error("FrameworkImageWebnizationScheduler cycle failed", ex);
        } finally {
            running.set(false);
            log.info("FrameworkImageWebnizationScheduler finished (elapsedMs={})", System.currentTimeMillis() - startedAt);
        }
    }
}
