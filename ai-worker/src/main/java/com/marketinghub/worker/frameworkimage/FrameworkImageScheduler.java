package com.marketinghub.worker.frameworkimage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicBoolean;

/** Responsabilidade: manter o agendador legado de framework-image disponível apenas por fallback operacional. */
@Component
@ConditionalOnProperty(prefix = "framework-image.legacy-scheduler", name = "enabled", havingValue = "true")
public class FrameworkImageScheduler {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageScheduler.class);

    private final FrameworkImageService service;
    private final boolean enabled;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Inicializa o scheduler legado com o serviço antigo e a chave operacional de habilitação. */
    public FrameworkImageScheduler(FrameworkImageService service,
                                   @Value("${framework-image.scheduler.enabled:true}") boolean enabled) {
        this.service = service;
        this.enabled = enabled;
    }

    /** Executa o ciclo legado somente quando o fallback antigo estiver explicitamente habilitado. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        if (!enabled) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("FrameworkImageScheduler previous cycle still running");
            return;
        }
        long startedAt = System.currentTimeMillis();
        try {
            service.processPending();
        } catch (Exception ex) {
            log.error("FrameworkImageScheduler cycle failed", ex);
            throw ex;
        } finally {
            running.set(false);
            log.info("FrameworkImageScheduler finished (elapsedMs={})", System.currentTimeMillis() - startedAt);
        }
    }
}
