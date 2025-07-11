package com.marketinghub.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SuccessProductScheduler {
    private static final Logger log = LoggerFactory.getLogger(SuccessProductScheduler.class);
    private final SuccessProductAnalyzer analyzer;

    public SuccessProductScheduler(SuccessProductAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        log.info("SuccessProductScheduler started");
        try {
            analyzer.analyzeNewProducts();
        } finally {
            log.info("SuccessProductScheduler finished");
        }
    }
}
