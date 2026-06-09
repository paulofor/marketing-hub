package com.marketinghub.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.openai.service.OpenAiModelPricingSyncService;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar a sincronização de preços oficiais OpenAI no catálogo financeiro local. */
@ExtendWith(MockitoExtension.class)
class OpenAiModelPricingSyncServiceTest {

    @Mock
    private OpenAiPricingPageClient pricingPageClient;

    @Mock
    private OpenAiModelRepository repository;

    /** Garante que variantes datadas recebem preços do código-base oficial mais específico sem perder identidade. */
    @Test
    void shouldRefreshDatedModelVariantWithBestOfficialBasePricing() {
        OpenAiModelPricing basePrice = new OpenAiModelPricing(
                "gpt-5.5",
                "gpt-5.5",
                new BigDecimal("5.00"),
                new BigDecimal("0.50"),
                new BigDecimal("30.00"),
                new BigDecimal("2.50"),
                new BigDecimal("0.25"),
                new BigDecimal("15.00"));
        OpenAiModel datedVariant = OpenAiModel.builder()
                .id(11L)
                .name("GPT 5.5 2026 04 23")
                .code("gpt-5.5-2026-04-23")
                .priceInputStandard(BigDecimal.ZERO)
                .priceInputCachedStandard(BigDecimal.ZERO)
                .priceOutputStandard(BigDecimal.ZERO)
                .priceInputBatch(BigDecimal.ZERO)
                .priceInputCachedBatch(BigDecimal.ZERO)
                .priceOutputBatch(BigDecimal.ZERO)
                .build();
        when(pricingPageClient.fetchAllModelPricing()).thenReturn(List.of(basePrice));
        when(pricingPageClient.findBestModelPricing(List.of(basePrice), datedVariant.getCode()))
                .thenReturn(Optional.of(basePrice));
        when(repository.findByCode("gpt-5.5")).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of(datedVariant));

        OpenAiModelPricingSyncService service = new OpenAiModelPricingSyncService(pricingPageClient, repository);
        int updated = service.syncOfficialPricing();

        assertThat(updated).isEqualTo(2);
        assertThat(datedVariant.getCode()).isEqualTo("gpt-5.5-2026-04-23");
        assertThat(datedVariant.getName()).isEqualTo("GPT 5.5 2026 04 23");
        assertThat(datedVariant.getPriceInputStandard()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(datedVariant.getPriceInputCachedStandard()).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(datedVariant.getPriceOutputStandard()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(datedVariant.getPriceInputBatch()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(datedVariant.getPriceInputCachedBatch()).isEqualByComparingTo(new BigDecimal("0.25"));
        assertThat(datedVariant.getPriceOutputBatch()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(datedVariant.getPricingSource()).isEqualTo("openai:/models");
        assertThat(datedVariant.getLastPricingSyncAt()).isNotNull();
        verify(repository).save(datedVariant);
    }

    /** Garante que modelos-base oficiais continuam sendo criados com identidade publicada na tabela de preços. */
    @Test
    void shouldPreserveBaseModelIdentityWhenUpsertingOfficialPrice() {
        OpenAiModelPricing basePrice = new OpenAiModelPricing(
                "gpt-5.5",
                "gpt-5.5",
                new BigDecimal("5.00"),
                new BigDecimal("0.50"),
                new BigDecimal("30.00"),
                new BigDecimal("2.50"),
                new BigDecimal("0.25"),
                new BigDecimal("15.00"));
        when(pricingPageClient.fetchAllModelPricing()).thenReturn(List.of(basePrice));
        when(repository.findByCode("gpt-5.5")).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of());

        OpenAiModelPricingSyncService service = new OpenAiModelPricingSyncService(pricingPageClient, repository);
        service.syncOfficialPricing();

        ArgumentCaptor<OpenAiModel> captor = ArgumentCaptor.forClass(OpenAiModel.class);
        verify(repository).save(captor.capture());
        OpenAiModel savedBase = captor.getValue();
        assertThat(savedBase.getCode()).isEqualTo("gpt-5.5");
        assertThat(savedBase.getName()).isEqualTo("gpt-5.5");
        assertThat(savedBase.getPriceOutputStandard()).isEqualByComparingTo(new BigDecimal("30.00"));
    }
}
