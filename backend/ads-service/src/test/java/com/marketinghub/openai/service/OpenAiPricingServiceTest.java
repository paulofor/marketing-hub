package com.marketinghub.openai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.openai.repository.OpenAiModelRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenAiPricingServiceTest {

    @Mock
    private OpenAiModelRepository modelRepository;

    private OpenAiPricingService service;

    @BeforeEach
    void setUp() {
        service = new OpenAiPricingService(modelRepository);
    }

    @Test
    void estimateStandardCostUsesPricesPerMillionTokens() {
        OpenAiModel model = OpenAiModel.builder()
                .code("gpt-5.3-codex")
                .priceInputStandard(new BigDecimal("2.00000"))
                .priceOutputStandard(new BigDecimal("10.00000"))
                .build();
        when(modelRepository.findByCode("gpt-5.3-codex")).thenReturn(Optional.of(model));

        OpenAiResponse.OpenAiUsage usage = new OpenAiResponse.OpenAiUsage(250_000, 125_000, null, null, 375_000);
        BigDecimal cost = service.estimateStandardCost("gpt-5.3-codex", usage);

        assertThat(cost).isEqualByComparingTo("1.7500");
    }

    @Test
    void estimateBatchCostUsesBatchColumnsPerMillionTokens() {
        OpenAiModel model = OpenAiModel.builder()
                .code("gpt-5.3-codex")
                .priceInputBatch(new BigDecimal("1.00000"))
                .priceOutputBatch(new BigDecimal("4.00000"))
                .build();
        when(modelRepository.findByCode("gpt-5.3-codex")).thenReturn(Optional.of(model));

        OpenAiResponse.OpenAiUsage usage = new OpenAiResponse.OpenAiUsage(300_000, 50_000, null, null, 350_000);
        BigDecimal cost = service.estimateBatchCost("gpt-5.3-codex", usage);

        assertThat(cost).isEqualByComparingTo("0.5000");
    }

    @Test
    void estimateCostFailsWhenModelIsMissingFromCatalog() {
        when(modelRepository.findByCode("missing-model")).thenReturn(Optional.empty());

        OpenAiResponse.OpenAiUsage usage = new OpenAiResponse.OpenAiUsage(1000, 1000, null, null, 2000);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.estimateStandardCost("missing-model", usage));

        assertThat(error.getMessage()).contains("Modelo OpenAI não encontrado no catálogo");
    }
}
