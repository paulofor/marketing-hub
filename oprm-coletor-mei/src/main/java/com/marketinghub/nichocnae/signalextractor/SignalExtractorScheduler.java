package com.marketinghub.nichocnae.signalextractor;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda a etapa cinco do pipeline OPRM NichoCNAE dentro do coletor OPRM. */
@Component
public class SignalExtractorScheduler {
    private static final Logger log = LoggerFactory.getLogger(SignalExtractorScheduler.class);
    private static final String REQUESTED_BY = "SCHEDULED_OPRM_SIGNAL_EXTRACTOR";

    private final SignalExtractorService signalExtractorService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Inicializa o scheduler com o serviço da etapa cinco que extrai sinais estruturados no coletor. */
    public SignalExtractorScheduler(SignalExtractorService signalExtractorService) {
        this.signalExtractorService = signalExtractorService;
    }

    /** Registra no boot o cron fixo que processa snapshots pendentes da etapa cinco. */
    @PostConstruct
    public void logScheduleConfiguration() {
        log.info(
                "Scheduler da etapa cinco OPRM NichoCNAE carregado (stage=oprmSignalExtractor, cron={}, requestedBy={})",
                "0 */4 * * * *",
                REQUESTED_BY);
    }

    /** Processa periodicamente snapshots COMPLETED gerados pela etapa quatro e grava sinais no backend. */
    @Scheduled(cron = "0 */4 * * * *")
    public void processPendingSnapshots() {
        if (!running.compareAndSet(false, true)) {
            log.info(
                    "Ignorando varredura concorrente da etapa cinco OPRM NichoCNAE porque uma execução anterior ainda está ativa (requestedBy={})",
                    REQUESTED_BY);
            return;
        }

        try {
            log.info("Iniciando varredura agendada da etapa cinco OPRM NichoCNAE (requestedBy={})", REQUESTED_BY);
            List<SignalExtractorOutput> outputs = signalExtractorService.processPending(REQUESTED_BY);
            log.info(
                    "Varredura agendada da etapa cinco OPRM NichoCNAE concluída (processedCount={}, requestedBy={})",
                    outputs.size(),
                    REQUESTED_BY);
        } catch (RuntimeException ex) {
            log.error("Erro na varredura agendada da etapa cinco OPRM NichoCNAE (requestedBy={})", REQUESTED_BY, ex);
            throw ex;
        } finally {
            running.set(false);
        }
    }
}
