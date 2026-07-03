package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.marketinghub.openai.service.OpenAiPricingService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Valida o cálculo de custos OpenAI da Biblioteca MOIS usando o serviço comum de IA.
 */
@ExtendWith(MockitoExtension.class)
class MoisSalesLibraryPricingServiceTest {

    @Mock
    private OpenAiPricingService openAiPricingService;

    @InjectMocks
    private MoisSalesLibraryPricingService service;

    /**
     * Garante cálculo flex usando o serviço comum de preços OpenAI do backend.
     */
    @Test
    void shouldEstimateBatchCostFromCommonOpenAiPricingService() {
        given(openAiPricingService.estimateFlexCost("gpt-5.2", 250_000, 125_000))
                .willReturn(new BigDecimal("1.5625"));

        BigDecimal cost = service.estimateBatchCost("gpt-5.2", 250_000, 125_000);

        assertThat(cost).isEqualByComparingTo("1.5625");
    }
}
