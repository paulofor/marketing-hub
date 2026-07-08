package com.marketinghub.openai;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.modelos.openai.catalogo.v1.service.OpenAiModelCatalogV1Service;
import com.marketinghub.openai.service.OpenAiModelPricingSyncService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Responsabilidade: validar a rotina diária que mantém modelos e preços OpenAI sincronizados. */
class OpenAiModelPricingSchedulerTest {

    /** Garante que a rotina diária atualize o catálogo técnico antes dos preços financeiros. */
    @Test
    void shouldSyncOfficialCatalogBeforePricing() {
        OpenAiModelCatalogV1Service catalogService = mock(OpenAiModelCatalogV1Service.class);
        OpenAiModelPricingSyncService syncService = mock(OpenAiModelPricingSyncService.class);
        when(syncService.syncOfficialPricing()).thenReturn(3);

        new OpenAiModelPricingScheduler(catalogService, syncService).syncDailyPricing();

        InOrder order = inOrder(catalogService, syncService);
        order.verify(catalogService).fetchAndPersistCatalog();
        order.verify(syncService).syncOfficialPricing();
    }
}
