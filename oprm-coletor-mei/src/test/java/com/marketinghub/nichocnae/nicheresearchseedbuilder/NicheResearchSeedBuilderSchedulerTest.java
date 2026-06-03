package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar o agendamento automático da etapa dois dentro do módulo OPRM. */
@ExtendWith(MockitoExtension.class)
class NicheResearchSeedBuilderSchedulerTest {
    private static final String REQUESTED_BY = "SCHEDULED_OPRM_NICHE_RESEARCH_SEED_BUILDER";

    @Mock private NicheResearchSeedBuilderService seedBuilderService;

    /** Confirma que o scheduler processa pendências usando o serviço da própria etapa OPRM. */
    @Test
    void shouldProcessPendingSeedsOnSchedule() {
        when(seedBuilderService.processPending(REQUESTED_BY)).thenReturn(List.of());
        NicheResearchSeedBuilderScheduler scheduler = new NicheResearchSeedBuilderScheduler(seedBuilderService);

        scheduler.processPendingSeeds();

        verify(seedBuilderService).processPending(REQUESTED_BY);
    }

    /** Confirma que falhas liberam a trava local para a próxima execução agendada. */
    @Test
    void shouldReleaseLocalGuardAfterFailure() {
        RuntimeException failure = new RuntimeException("OpenAI indisponível");
        doThrow(failure).doReturn(List.of()).when(seedBuilderService).processPending(REQUESTED_BY);
        NicheResearchSeedBuilderScheduler scheduler = new NicheResearchSeedBuilderScheduler(seedBuilderService);

        assertThatThrownBy(scheduler::processPendingSeeds).isSameAs(failure);
        scheduler.processPendingSeeds();

        verify(seedBuilderService, org.mockito.Mockito.times(2)).processPending(REQUESTED_BY);
    }
}
