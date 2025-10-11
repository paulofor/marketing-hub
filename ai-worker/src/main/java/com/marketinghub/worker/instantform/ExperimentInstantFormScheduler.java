package com.marketinghub.worker.instantform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agenda a geração periódica de Instant Forms.
 */
@Component
public class ExperimentInstantFormScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExperimentInstantFormScheduler.class);
    private final ExperimentInstantFormService service;

    public ExperimentInstantFormScheduler(ExperimentInstantFormService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        long start = System.currentTimeMillis();
        log.info("ExperimentInstantFormScheduler iniciado");
        try {
            service.generate();
        } catch (Exception ex) {
            log.error("ExperimentInstantFormScheduler falhou", ex);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("ExperimentInstantFormScheduler finalizado em {} ms", elapsed);
        }
    }
}
