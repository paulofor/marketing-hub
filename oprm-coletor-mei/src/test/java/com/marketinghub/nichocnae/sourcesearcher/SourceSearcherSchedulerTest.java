package com.marketinghub.nichocnae.sourcesearcher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar o agendamento automático da etapa três dentro do módulo OPRM. */
@ExtendWith(MockitoExtension.class)
class SourceSearcherSchedulerTest {
    private static final String REQUESTED_BY = "SCHEDULED_OPRM_SOURCE_SEARCHER";

    @Mock private SourceSearcherService sourceSearcherService;

    /** Confirma que o scheduler processa queries pendentes usando o serviço da própria etapa OPRM. */
    @Test
    void shouldProcessPendingQueriesOnSchedule() {
        when(sourceSearcherService.processPending(REQUESTED_BY)).thenReturn(List.of());
        SourceSearcherScheduler scheduler = new SourceSearcherScheduler(sourceSearcherService);

        scheduler.processPendingQueries();

        verify(sourceSearcherService).processPending(REQUESTED_BY);
    }

    /** Confirma que falhas liberam a trava local para a próxima execução agendada. */
    @Test
    void shouldReleaseLocalGuardAfterFailure() {
        RuntimeException failure = new RuntimeException("Busca indisponível");
        doThrow(failure).doReturn(List.of()).when(sourceSearcherService).processPending(REQUESTED_BY);
        SourceSearcherScheduler scheduler = new SourceSearcherScheduler(sourceSearcherService);

        assertThatThrownBy(scheduler::processPendingQueries).isSameAs(failure);
        scheduler.processPendingQueries();

        verify(sourceSearcherService, org.mockito.Mockito.times(2)).processPending(REQUESTED_BY);
    }
}
