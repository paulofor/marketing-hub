package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda a etapa dois do pipeline OPRM NichoCNAE dentro do próprio coletor OPRM. */
@Component
public class NicheResearchSeedBuilderScheduler {
    private static final Logger log = LoggerFactory.getLogger(NicheResearchSeedBuilderScheduler.class);
    private static final String REQUESTED_BY = "SCHEDULED_OPRM_NICHE_RESEARCH_SEED_BUILDER";

    private final NicheResearchSeedBuilderService seedBuilderService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Inicializa o scheduler com o serviço da etapa dois que acessa o modelo no módulo OPRM. */
    public NicheResearchSeedBuilderScheduler(NicheResearchSeedBuilderService seedBuilderService) {
        this.seedBuilderService = seedBuilderService;
    }

    /** Registra no boot o cron fixo que processa seeds pendentes sem depender do ai-worker. */
    @PostConstruct
    public void logScheduleConfiguration() {
        log.info(
                "Scheduler da etapa dois OPRM NichoCNAE carregado (stage=oprmNicheResearchSeedBuilder, cron={}, requestedBy={})",
                "0 */1 * * * *",
                REQUESTED_BY);
    }

    /** Processa periodicamente ciclos RUNNING sem seed, gerando seed e queries pelo próprio módulo OPRM. */
    @Scheduled(cron = "0 */1 * * * *")
    public void processPendingSeeds() {
        if (!running.compareAndSet(false, true)) {
            log.info(
                    "Ignorando varredura concorrente da etapa dois OPRM NichoCNAE porque uma execução anterior ainda está ativa (requestedBy={})",
                    REQUESTED_BY);
            return;
        }

        try {
            log.info("Iniciando varredura agendada da etapa dois OPRM NichoCNAE (requestedBy={})", REQUESTED_BY);
            List<NicheResearchSeedBuilderOutput> outputs = seedBuilderService.processPending(REQUESTED_BY);
            log.info(
                    "Varredura agendada da etapa dois OPRM NichoCNAE concluída (processedCount={}, requestedBy={})",
                    outputs.size(),
                    REQUESTED_BY);
        } catch (RuntimeException ex) {
            log.error("Erro na varredura agendada da etapa dois OPRM NichoCNAE (requestedBy={})", REQUESTED_BY, ex);
            throw ex;
        } finally {
            running.set(false);
        }
    }
}
