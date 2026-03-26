package com.marketinghub.salesvideo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Scheduler responsável por disparar a limpeza automática de assets órfãos.
 */
@Component
public class SalesVideoAssetCleanupScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SalesVideoAssetCleanupScheduler.class);

    private final SalesVideoAssetCleanupService cleanupService;
    private final boolean enabled;
    private final int retentionDays;
    private final int batchSize;

    public SalesVideoAssetCleanupScheduler(SalesVideoAssetCleanupService cleanupService,
                                           @Value("${sales-video.assets.cleanup.enabled:true}") boolean enabled,
                                           @Value("${sales-video.assets.cleanup.retention-days:7}") int retentionDays,
                                           @Value("${sales-video.assets.cleanup.batch-size:250}") int batchSize) {
        this.cleanupService = cleanupService;
        this.enabled = enabled;
        this.retentionDays = retentionDays;
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "${sales-video.assets.cleanup.cron:0 0 * * * *}")
    public void cleanupOrphans() {
        if (!enabled) {
            return;
        }
        int removed = cleanupService.cleanup(Duration.ofDays(retentionDays), batchSize);
        if (removed > 0) {
            LOGGER.info("Limpeza de assets de vídeo removeu {} registros órfãos", removed);
        }
    }
}
