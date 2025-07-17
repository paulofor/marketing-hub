package com.marketinghub.experiment.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic task that would synchronize metrics with Meta Marketing API.
 */
@Component
public class MetricSyncScheduler {
    private final MetricService metricService;

    public MetricSyncScheduler(MetricService metricService) {
        this.metricService = metricService;
    }

    /**
     * Dummy scheduled task for metrics sync.
     */
    @Scheduled(fixedDelay = 3600000)
    public void syncMetrics() {
        // TODO connect to Meta API and store MetricSnapshot
    }
}
