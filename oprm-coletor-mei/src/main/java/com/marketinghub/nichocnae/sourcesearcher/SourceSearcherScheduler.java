package com.marketinghub.nichocnae.sourcesearcher;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda a etapa três do pipeline OPRM NichoCNAE dentro do coletor OPRM. */
@Component
public class SourceSearcherScheduler {
    private static final Logger log = LoggerFactory.getLogger(SourceSearcherScheduler.class);
    private static final String REQUESTED_BY = "SCHEDULED_OPRM_SOURCE_SEARCHER";

    private final SourceSearcherService sourceSearcherService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Inicializa o scheduler com o serviço da etapa três que executa busca pública no coletor. */
    public SourceSearcherScheduler(SourceSearcherService sourceSearcherService) {
        this.sourceSearcherService = sourceSearcherService;
    }

    /** Registra no boot o cron fixo que processa queries pendentes da etapa três. */
    @PostConstruct
    public void logScheduleConfiguration() {
        log.info(
                "Scheduler da etapa três OPRM NichoCNAE carregado (stage=oprmSourceSearcher, cron={}, requestedBy={})",
                "0 */2 * * * *",
                REQUESTED_BY);
    }

    /** Processa periodicamente queries PENDING geradas pela etapa dois e grava fontes candidatas no backend. */
    @Scheduled(cron = "0 */2 * * * *")
    public void processPendingQueries() {
        if (!running.compareAndSet(false, true)) {
            log.info(
                    "Ignorando varredura concorrente da etapa três OPRM NichoCNAE porque uma execução anterior ainda está ativa (requestedBy={})",
                    REQUESTED_BY);
            return;
        }

        try {
            log.info("Iniciando varredura agendada da etapa três OPRM NichoCNAE (requestedBy={})", REQUESTED_BY);
            List<SourceSearcherOutput> outputs = sourceSearcherService.processPending(REQUESTED_BY);
            log.info(
                    "Varredura agendada da etapa três OPRM NichoCNAE concluída (processedCount={}, requestedBy={})",
                    outputs.size(),
                    REQUESTED_BY);
        } catch (RuntimeException ex) {
            log.error("Erro na varredura agendada da etapa três OPRM NichoCNAE (requestedBy={})", REQUESTED_BY, ex);
            throw ex;
        } finally {
            running.set(false);
        }
    }
}
