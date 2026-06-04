package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageBackfillGateway;
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
    private MoisSalesPageBackfillGateway gateway;

    @InjectMocks
    private MoisSalesPageBackfillService service;

    /**
     * Garante que o backfill consolida páginas, execuções e ponteiros gerando contadores finais.
     */
    @Test
    void shouldExecuteBackfillAndReturnCounters() {
        when(gateway.countLegacyUrlIngests()).thenReturn(145L);
        when(gateway.countSalesPages()).thenReturn(0L, 145L);
        when(gateway.countJobExecutions()).thenReturn(0L, 420L);
        when(gateway.backfillSalesPages()).thenReturn(145);
        when(gateway.backfillLatestProcessingJobs()).thenReturn(145);
        when(gateway.backfillLatestAnalyses()).thenReturn(145);
        when(gateway.backfillLatestSnapshots()).thenReturn(130);
        when(gateway.backfillLatestCollectedReferenceHtmlCaptures()).thenReturn(0);
        when(gateway.updateLastJobExecutionPointers()).thenReturn(145);

        MoisSalesPageBackfillService.BackfillCounters counters = service.executeBackfill();

        assertThat(counters.legacyPages()).isEqualTo(145);
        assertThat(counters.salesPagesBefore()).isZero();
        assertThat(counters.salesPagesAfter()).isEqualTo(145);
        assertThat(counters.jobExecutionsAfter()).isEqualTo(420);
        assertThat(counters.processingJobsInserted()).isEqualTo(145);
        assertThat(counters.snapshotsInserted()).isEqualTo(130);
        verify(gateway).updateLastJobExecutionPointers();
    }

    /**
     * Garante que a rotina pode ser desligada por configuração sem tocar no banco.
     */
    @Test
    void shouldSkipStartupBackfillWhenDisabled() throws Exception {
        setEnabled(false);

        service.run(new DefaultApplicationArguments());

        verify(gateway, never()).backfillSalesPages();
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
