package com.marketinghub.nichocnae.routineresearchorchestrator;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida o agendamento inicial da etapa zero do pipeline OPRM nichocnae. */
@ExtendWith(MockitoExtension.class)
class RoutineResearchOrchestratorInitialSchedulerTest {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Mock private RoutineResearchOrchestratorService orchestratorService;

    /** Confirma que o scheduler dispara a etapa zero na data operacional de 03/06/2026. */
    @Test
    void shouldRunInitialScheduleOnConfiguredDate() {
        when(orchestratorService.runNext("SCHEDULED_INITIAL_NICHO_CNAE_2026_06_03_22H"))
                .thenReturn(new RoutineResearchOrchestratorOutput(
                        true,
                        101L,
                        55L,
                        "9602501",
                        "Cabeleireiros, manicure e pedicure",
                        "Agenda cheia para manicures",
                        null,
                        "AUTO_SCORE_QUEUE",
                        "RUNNING",
                        "RESEARCH_RUNNING",
                        "Pesquisa iniciada."));
        RoutineResearchOrchestratorInitialScheduler scheduler = newScheduler("2026-06-04T01:00:00Z");

        scheduler.runInitialNichoCnaeSchedule();

        verify(orchestratorService).runNext("SCHEDULED_INITIAL_NICHO_CNAE_2026_06_03_22H");
    }

    /** Confirma que disparos anuais futuros do mesmo cron são ignorados pela guarda de data. */
    @Test
    void shouldIgnoreScheduleOutsideConfiguredDate() {
        RoutineResearchOrchestratorInitialScheduler scheduler = newScheduler("2027-06-04T01:00:00Z");

        scheduler.runInitialNichoCnaeSchedule();

        verify(orchestratorService, never()).runNext("SCHEDULED_INITIAL_NICHO_CNAE_2026_06_03_22H");
    }

    /** Monta o scheduler com relógio fixo no fuso de São Paulo para validar o comportamento temporal. */
    private RoutineResearchOrchestratorInitialScheduler newScheduler(String instant) {
        return new RoutineResearchOrchestratorInitialScheduler(
                orchestratorService, Clock.fixed(Instant.parse(instant), SAO_PAULO));
    }
}
