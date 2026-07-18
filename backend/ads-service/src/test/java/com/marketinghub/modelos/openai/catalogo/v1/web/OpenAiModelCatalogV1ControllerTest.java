package com.marketinghub.modelos.openai.catalogo.v1.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.modelos.openai.catalogo.v1.service.OpenAiModelCatalogV1Service;
import com.marketinghub.openai.OpenAiPricingPageClient;
import com.marketinghub.openai.dto.OpenAiModelPricingSyncResponse;
import com.marketinghub.openai.mapper.OpenAiModelMapper;
import com.marketinghub.openai.service.OpenAiModelPricingSyncService;
import com.marketinghub.openai.service.OpenAiModelService;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar contratos administrativos do catálogo OpenAI expostos ao frontend. */
class OpenAiModelCatalogV1ControllerTest {

    /** Garante que o endpoint manual dispare a sincronização financeira de preços oficiais. */
    @Test
    void shouldTriggerManualPricingSync() {
        OpenAiModelPricingSyncService pricingSyncService = mock(OpenAiModelPricingSyncService.class);
        when(pricingSyncService.syncOfficialPricing()).thenReturn(35);
        OpenAiModelCatalogV1Controller controller = new OpenAiModelCatalogV1Controller(
                mock(OpenAiModelService.class),
                mock(OpenAiModelMapper.class),
                mock(OpenAiModelCatalogV1Service.class),
                pricingSyncService);

        OpenAiModelPricingSyncResponse response = controller.syncPricing();

        assertThat(response.modelsUpdated()).isEqualTo(35);
        assertThat(response.source()).isEqualTo(OpenAiPricingPageClient.PRICING_PAGE_URL);
        assertThat(response.syncedAt()).isNotNull();
        verify(pricingSyncService).syncOfficialPricing();
    }
}
