package com.marketinghub.worker.successproduct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically triggers the {@link SuccessProductNicheHypothesisService} to
 * generate market niches and hypotheses from success products.
 */
@Component
public class SuccessProductNicheHypothesisScheduler {
    private static final Logger log = LoggerFactory.getLogger(SuccessProductNicheHypothesisScheduler.class);
    private final SuccessProductNicheHypothesisService service;

    public SuccessProductNicheHypothesisScheduler(SuccessProductNicheHypothesisService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        log.info("SuccessProductNicheHypothesisScheduler started");
        try {
            service.generate();
        } finally {
            log.info("SuccessProductNicheHypothesisScheduler finished");
        }
    }
}

