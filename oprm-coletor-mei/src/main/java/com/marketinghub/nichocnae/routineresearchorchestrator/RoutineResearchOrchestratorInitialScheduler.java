package com.marketinghub.nichocnae.routineresearchorchestrator;

import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private static final String REQUESTED_BY = "SCHEDULED_INITIAL_NICHO_CNAE_2026_06_03_04H";

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

    /** Registra no boot a configuração temporal do agendamento inicial para diagnóstico operacional. */
    @PostConstruct
    public void logInitialScheduleConfiguration() {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(SCHEDULE_ZONE));
        log.info(
                "Scheduler inicial NichoCNAE carregado (cron={}, zone={}, expectedDate={}, currentDateTime={}, requestedBy={})",
                "0 0 4 3 6 *",
                SCHEDULE_ZONE,
                INITIAL_SCHEDULE_DATE,
                now,
                REQUESTED_BY);
    }

    /** Dispara a etapa zero do NichoCNAE em 03/06/2026 às 04h no horário de São Paulo. */
    @Scheduled(cron = "0 0 4 3 6 *", zone = "America/Sao_Paulo")
    public void runInitialNichoCnaeSchedule() {
        ZonedDateTime currentDateTime = ZonedDateTime.now(clock.withZone(SCHEDULE_ZONE));
        LocalDate currentDate = currentDateTime.toLocalDate();
        log.info(
                "Cron inicial NichoCNAE acionado (currentDateTime={}, expectedDate={}, requestedBy={})",
                currentDateTime,
                INITIAL_SCHEDULE_DATE,
                REQUESTED_BY);
        if (!INITIAL_SCHEDULE_DATE.equals(currentDate)) {
            log.info(
                    "Ignorando agendamento inicial NichoCNAE fora da data planejada (currentDate={}, expectedDate={})",
                    currentDate,
                    INITIAL_SCHEDULE_DATE);
            return;
        }

        try {
            log.info(
                    "Iniciando agendamento inicial NichoCNAE às 04h America/Sao_Paulo (currentDateTime={}, requestedBy={}).",
                    currentDateTime,
                    REQUESTED_BY);
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
