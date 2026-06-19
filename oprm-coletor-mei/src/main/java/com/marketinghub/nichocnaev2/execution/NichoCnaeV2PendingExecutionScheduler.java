package com.marketinghub.nichocnaev2.execution;

import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda a busca de pendências de todas as etapas do pipeline NichoCNAE v2 no executor OPRM. */
@Component
public class NichoCnaeV2PendingExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(NichoCnaeV2PendingExecutionScheduler.class);
    private static final String REQUESTED_BY = "SCHEDULED_OPRM_NICHO_CNAE_V2_PENDING_EXECUTION";

    private final NichoCnaeV2PendingExecutionService executionService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Inicializa o scheduler com o serviço que consulta e executa pendências v2. */
    public NichoCnaeV2PendingExecutionScheduler(NichoCnaeV2PendingExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Registra no boot o cron fixo de três minutos usado pela varredura v2. */
    @PostConstruct
    public void logScheduleConfiguration() {
        log.info(
                "Scheduler NichoCNAE v2 carregado para consultar pendências de todas as etapas (cron={}, requestedBy={})",
                "0 */3 * * * *",
                REQUESTED_BY);
    }

    /** Consulta a cada três minutos os endpoints pending de todas as etapas NichoCNAE v2. */
    @Scheduled(cron = "0 */3 * * * *")
    public void processPendingStageExecutions() {
        if (!running.compareAndSet(false, true)) {
            log.info("Ignorando varredura concorrente NichoCNAE v2 porque outra execução está ativa (requestedBy={})", REQUESTED_BY);
            return;
        }
        try {
            log.info("Iniciando varredura agendada NichoCNAE v2 de pendências (requestedBy={})", REQUESTED_BY);
            int processedCount = executionService.processAllPending();
            log.info(
                    "Varredura agendada NichoCNAE v2 concluída (processedCount={}, requestedBy={})",
                    processedCount,
                    REQUESTED_BY);
        } catch (RuntimeException ex) {
            log.error("Erro na varredura agendada NichoCNAE v2 de pendências (requestedBy={})", REQUESTED_BY, ex);
            throw ex;
        } finally {
            running.set(false);
        }
    }
}
