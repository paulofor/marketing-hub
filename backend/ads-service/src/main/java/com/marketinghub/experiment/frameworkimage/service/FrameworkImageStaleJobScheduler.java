package com.marketinghub.experiment.frameworkimage.service;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FrameworkImageStaleJobScheduler {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageStaleJobScheduler.class);

    private final FrameworkImageGenerationService service;
    private final boolean enabled;
    private final Duration staleTimeout;
    private final int batchSize;

    public FrameworkImageStaleJobScheduler(FrameworkImageGenerationService service,
                                           @Value("${framework-image.stale-guard.enabled:true}") boolean enabled,
                                           @Value("${framework-image.stale-guard.timeout:PT30M}") Duration staleTimeout,
                                           @Value("${framework-image.stale-guard.batch-size:50}") int batchSize) {
        this.service = service;
        this.enabled = enabled;
        this.staleTimeout = staleTimeout;
        this.batchSize = Math.max(1, batchSize);
    }

    @Scheduled(fixedDelayString = "${framework-image.stale-guard.fixed-delay-ms:60000}")
    public void run() {
        if (!enabled) {
            return;
        }
        Instant staleBefore = Instant.now().minus(staleTimeout);
        int failedCount = service.failStaleProcessingJobs(
                staleBefore,
                batchSize,
                "Job expirado por timeout de processamento");
        if (failedCount > 0) {
            log.warn("framework-image-stale-guard failedCount={} staleBefore={}", failedCount, staleBefore);
        }
    }
}
