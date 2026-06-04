package com.marketinghub.repository.jdbc.mois;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

/**
 * Valida a orquestração idempotente do backfill inicial das páginas de venda MOIS.
 */
@ExtendWith(MockitoExtension.class)
class MoisSalesPageBackfillServiceTest {

    @Mock
    private MoisSalesPageBackfillRepository repository;

    @InjectMocks
    private MoisSalesPageBackfillService service;

    /**
     * Garante que o backfill consolida páginas, execuções e ponteiros gerando contadores finais.
     */
    @Test
    void shouldExecuteBackfillAndReturnCounters() {
        when(repository.countLegacyUrlIngests()).thenReturn(145L);
        when(repository.countSalesPages()).thenReturn(0L, 145L);
        when(repository.countJobExecutions()).thenReturn(0L, 420L);
        when(repository.backfillSalesPages()).thenReturn(145);
        when(repository.backfillLatestProcessingJobs()).thenReturn(145);
        when(repository.backfillLatestAnalyses()).thenReturn(145);
        when(repository.backfillLatestSnapshots()).thenReturn(130);
        when(repository.backfillLatestCollectedReferenceHtmlCaptures()).thenReturn(0);
        when(repository.updateLastJobExecutionPointers()).thenReturn(145);

        MoisSalesPageBackfillService.BackfillCounters counters = service.executeBackfill();

        assertThat(counters.legacyPages()).isEqualTo(145);
        assertThat(counters.salesPagesBefore()).isZero();
        assertThat(counters.salesPagesAfter()).isEqualTo(145);
        assertThat(counters.jobExecutionsAfter()).isEqualTo(420);
        assertThat(counters.processingJobsInserted()).isEqualTo(145);
        assertThat(counters.snapshotsInserted()).isEqualTo(130);
        verify(repository).updateLastJobExecutionPointers();
    }

    /**
     * Garante que a rotina pode ser desligada por configuração sem tocar no banco.
     */
    @Test
    void shouldSkipStartupBackfillWhenDisabled() throws Exception {
        setEnabled(false);

        service.run(new DefaultApplicationArguments());

        verify(repository, never()).backfillSalesPages();
    }

    /**
     * Ajusta a propriedade privada simulando a injeção de configuração do Spring.
     */
    private void setEnabled(boolean enabled) throws Exception {
        Field field = MoisSalesPageBackfillService.class.getDeclaredField("enabled");
        field.setAccessible(true);
        field.set(service, enabled);
    }
}
