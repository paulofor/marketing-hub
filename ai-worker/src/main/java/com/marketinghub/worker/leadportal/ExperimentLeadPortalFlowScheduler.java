package com.marketinghub.worker.leadportal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agenda a geração automática de fluxos do portal do lead.
 */
@Component
public class ExperimentLeadPortalFlowScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExperimentLeadPortalFlowScheduler.class);
    private final ExperimentLeadPortalFlowService service;

    public ExperimentLeadPortalFlowScheduler(ExperimentLeadPortalFlowService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void run() {
        long start = System.currentTimeMillis();
        log.info("ExperimentLeadPortalFlowScheduler iniciado");
        try {
            service.generate();
        } catch (Exception ex) {
            log.error("ExperimentLeadPortalFlowScheduler falhou", ex);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("ExperimentLeadPortalFlowScheduler finalizado em {} ms", elapsed);
        }
    }
}
