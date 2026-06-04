package com.marketinghub.nichocnae.sourcefetcher;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda a etapa quatro do pipeline OPRM NichoCNAE dentro do coletor OPRM. */
@Component
public class SourceFetcherScheduler {
    private static final Logger log = LoggerFactory.getLogger(SourceFetcherScheduler.class);
    private static final String REQUESTED_BY = "SCHEDULED_OPRM_SOURCE_FETCHER";

    private final SourceFetcherService sourceFetcherService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Inicializa o scheduler com o serviço da etapa quatro que coleta snapshots curtos no coletor. */
    public SourceFetcherScheduler(SourceFetcherService sourceFetcherService) {
        this.sourceFetcherService = sourceFetcherService;
    }

    /** Registra no boot o cron fixo que processa fontes candidatas pendentes da etapa quatro. */
    @PostConstruct
    public void logScheduleConfiguration() {
        log.info(
                "Scheduler da etapa quatro OPRM NichoCNAE carregado (stage=oprmSourceFetcher, cron={}, requestedBy={})",
                "30 */3 * * * *",
                REQUESTED_BY);
    }

    /** Processa periodicamente fontes FOUND geradas pela etapa três e grava snapshots curtos no backend. */
    @Scheduled(cron = "30 */3 * * * *")
    public void processPendingSources() {
        if (!running.compareAndSet(false, true)) {
            log.info(
                    "Ignorando varredura concorrente da etapa quatro OPRM NichoCNAE porque uma execução anterior ainda está ativa (requestedBy={})",
                    REQUESTED_BY);
            return;
        }

        try {
            log.info("Iniciando varredura agendada da etapa quatro OPRM NichoCNAE (requestedBy={})", REQUESTED_BY);
            List<SourceFetcherOutput> outputs = sourceFetcherService.processPending(REQUESTED_BY);
            log.info(
                    "Varredura agendada da etapa quatro OPRM NichoCNAE concluída (processedCount={}, requestedBy={})",
                    outputs.size(),
                    REQUESTED_BY);
        } catch (RuntimeException ex) {
            log.error("Erro na varredura agendada da etapa quatro OPRM NichoCNAE (requestedBy={})", REQUESTED_BY, ex);
            throw ex;
        } finally {
            running.set(false);
        }
    }
}
