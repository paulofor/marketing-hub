package com.marketinghub.nichocnae.sourcefetcher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar o agendamento automático da etapa quatro dentro do módulo OPRM. */
@ExtendWith(MockitoExtension.class)
class SourceFetcherSchedulerTest {
    private static final String REQUESTED_BY = "SCHEDULED_OPRM_SOURCE_FETCHER";

    @Mock private SourceFetcherService sourceFetcherService;

    /** Confirma que o scheduler processa fontes pendentes usando o serviço da própria etapa OPRM. */
    @Test
    void shouldProcessPendingSourcesOnSchedule() {
        when(sourceFetcherService.processPending(REQUESTED_BY)).thenReturn(List.of());
        SourceFetcherScheduler scheduler = new SourceFetcherScheduler(sourceFetcherService);

        scheduler.processPendingSources();

        verify(sourceFetcherService).processPending(REQUESTED_BY);
    }

    /** Confirma que falhas liberam a trava local para a próxima execução agendada. */
    @Test
    void shouldReleaseLocalGuardAfterFailure() {
        RuntimeException failure = new RuntimeException("Coleta indisponível");
        doThrow(failure).doReturn(List.of()).when(sourceFetcherService).processPending(REQUESTED_BY);
        SourceFetcherScheduler scheduler = new SourceFetcherScheduler(sourceFetcherService);

        assertThatThrownBy(scheduler::processPendingSources).isSameAs(failure);
        scheduler.processPendingSources();

        verify(sourceFetcherService, org.mockito.Mockito.times(2)).processPending(REQUESTED_BY);
    }
}
