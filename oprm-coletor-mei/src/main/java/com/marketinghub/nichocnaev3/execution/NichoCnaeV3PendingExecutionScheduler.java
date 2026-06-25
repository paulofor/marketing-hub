package com.marketinghub.nichocnaev3.execution;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda a varredura de pendências do pipeline NichoCNAE v3 no executor OPRM. */
@Component
public class NichoCnaeV3PendingExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(NichoCnaeV3PendingExecutionScheduler.class);
    private final NichoCnaeV3PendingExecutionService service;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Inicializa o scheduler com o serviço de execução v3. */
    public NichoCnaeV3PendingExecutionScheduler(NichoCnaeV3PendingExecutionService service) {
        this.service = service;
    }

    /** Consulta a cada três minutos os endpoints pending de todas as etapas v3. */
    @Scheduled(cron = "0 */3 * * * *")
    public void processPendingStageExecutions() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            int processed = service.processAllPending();
            log.info("Varredura NichoCNAE v3 concluída (processed={})", processed);
        } finally {
            running.set(false);
        }
    }
}
