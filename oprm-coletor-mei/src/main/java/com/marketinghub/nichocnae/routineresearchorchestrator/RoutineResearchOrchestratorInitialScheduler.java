package com.marketinghub.nichocnae.routineresearchorchestrator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda o disparo inicial único da etapa zero do pipeline NichoCNAE. */
@Component
public class RoutineResearchOrchestratorInitialScheduler {
    private static final Logger log = LoggerFactory.getLogger(RoutineResearchOrchestratorInitialScheduler.class);
    private static final ZoneId SCHEDULE_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate INITIAL_SCHEDULE_DATE = LocalDate.of(2026, 6, 3);
    private static final String REQUESTED_BY = "SCHEDULED_INITIAL_NICHO_CNAE_2026_06_03_22H";

    private final RoutineResearchOrchestratorService orchestratorService;
    private final Clock clock;

    /** Inicializa o scheduler inicial com o serviço da etapa zero e o relógio oficial de São Paulo. */
    @Autowired
    public RoutineResearchOrchestratorInitialScheduler(RoutineResearchOrchestratorService orchestratorService) {
        this(orchestratorService, Clock.system(SCHEDULE_ZONE));
    }

    /** Inicializa o scheduler com relógio explícito para permitir validação determinística da data de disparo. */
    RoutineResearchOrchestratorInitialScheduler(RoutineResearchOrchestratorService orchestratorService, Clock clock) {
        this.orchestratorService = orchestratorService;
        this.clock = clock;
    }

    /** Dispara a etapa zero do NichoCNAE em 03/06/2026 às 22h no horário de São Paulo. */
    @Scheduled(cron = "0 0 22 3 6 *", zone = "America/Sao_Paulo")
    public void runInitialNichoCnaeSchedule() {
        LocalDate currentDate = LocalDate.now(clock.withZone(SCHEDULE_ZONE));
        if (!INITIAL_SCHEDULE_DATE.equals(currentDate)) {
            log.info(
                    "Ignorando agendamento inicial NichoCNAE fora da data planejada (currentDate={}, expectedDate={})",
                    currentDate,
                    INITIAL_SCHEDULE_DATE);
            return;
        }

        try {
            log.info("Iniciando agendamento inicial NichoCNAE às 22h America/Sao_Paulo.");
            RoutineResearchOrchestratorOutput output = orchestratorService.runNext(REQUESTED_BY);
            log.info(
                    "Agendamento inicial NichoCNAE concluído (started={}, researchCycleId={}, sourceNicheId={}, message={})",
                    output.started(),
                    output.researchCycleId(),
                    output.sourceNicheId(),
                    output.message());
        } catch (RuntimeException ex) {
            log.error("Erro no agendamento inicial NichoCNAE (requestedBy={})", REQUESTED_BY, ex);
            throw ex;
        }
    }
}
