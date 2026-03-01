package com.marketinghub.worker.leadportal.style;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LeadPortalSimpleFormStyleGenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalSimpleFormStyleGenerationScheduler.class);

    private final LeadPortalSimpleFormStyleGenerationService service;

    public LeadPortalSimpleFormStyleGenerationScheduler(LeadPortalSimpleFormStyleGenerationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${lead-portal.simple-form-style.fixed-delay:60000}")
    public void run() {
        log.info("LeadPortalSimpleFormStyleGenerationScheduler iniciado");
        long start = System.currentTimeMillis();
        try {
            service.processPending();
        } catch (Exception ex) {
            log.error("LeadPortalSimpleFormStyleGenerationScheduler falhou", ex);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("LeadPortalSimpleFormStyleGenerationScheduler finalizado em {} ms", elapsed);
        }
    }
}
