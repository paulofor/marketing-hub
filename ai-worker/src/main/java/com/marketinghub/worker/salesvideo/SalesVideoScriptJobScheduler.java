package com.marketinghub.worker.salesvideo;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler responsável por disparar o processamento periódico dos jobs OpenAI do módulo de vídeo.
 */
@Component
public class SalesVideoScriptJobScheduler {
    private static final Logger log = LoggerFactory.getLogger(SalesVideoScriptJobScheduler.class);

    private final SalesVideoScriptJobService jobService;
    private final boolean enabled;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean loggedDisabled = new AtomicBoolean(false);

    public SalesVideoScriptJobScheduler(SalesVideoScriptJobService jobService,
                                        @Value("${sales-video.script.enabled:true}") boolean enabled) {
        this.jobService = jobService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${sales-video.script.fixed-delay:45000}")
    public void schedule() {
        if (!enabled) {
            if (!loggedDisabled.getAndSet(true)) {
                log.info("Processamento de Avatar Sales Video desabilitado via configuração");
            }
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("Execução anterior do job de sales video ainda em andamento");
            return;
        }
        try {
            jobService.processPendingScriptJobs();
        } finally {
            running.set(false);
        }
    }
}
