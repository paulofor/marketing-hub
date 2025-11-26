package com.marketinghub.worker.leadportal.image;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LeadPortalImageProcessingScheduler {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalImageProcessingScheduler.class);

    private final LeadPortalImageProcessingService processingService;

    public LeadPortalImageProcessingScheduler(LeadPortalImageProcessingService processingService) {
        this.processingService = processingService;
    }

    @Scheduled(fixedDelayString = "${lead-portal.image-processing.fixed-delay:60000}")
    public void processPackages() {
        List<?> processed = processingService.process();
        if (!processed.isEmpty()) {
            log.info("Processed {} lead-portal image package(s)", processed.size());
        }
    }
}
