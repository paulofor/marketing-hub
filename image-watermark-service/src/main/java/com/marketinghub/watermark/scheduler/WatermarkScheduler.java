package com.marketinghub.watermark.scheduler;

import com.marketinghub.watermark.service.WatermarkProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WatermarkScheduler {

    private static final Logger log = LoggerFactory.getLogger(WatermarkScheduler.class);

    private final WatermarkProcessingService watermarkProcessingService;

    public WatermarkScheduler(WatermarkProcessingService watermarkProcessingService) {
        this.watermarkProcessingService = watermarkProcessingService;
    }

    @Scheduled(fixedDelayString = "${watermark.scheduler.delay:60000}")
    public void run() {
        try {
            watermarkProcessingService.processPendingPackages();
        } catch (Exception ex) {
            log.error("Erro inesperado ao aplicar marca d'água em pacotes pendentes", ex);
        }
    }
}
