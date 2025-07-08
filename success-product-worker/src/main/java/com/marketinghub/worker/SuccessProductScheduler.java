package com.marketinghub.worker;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SuccessProductScheduler {
    private final SuccessProductAnalyzer analyzer;

    public SuccessProductScheduler(SuccessProductAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void run() {
        analyzer.analyzeNewProducts();
    }
}
