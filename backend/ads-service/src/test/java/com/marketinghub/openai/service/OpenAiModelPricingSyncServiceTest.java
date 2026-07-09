package com.marketinghub.openai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.OpenAiModelPricing;
import com.marketinghub.openai.OpenAiPricingPageClient;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a sincronização transacional de preços oficiais OpenAI. */
class OpenAiModelPricingSyncServiceTest {

    /** Garante que a sincronização atualize modelos pelo código canônico retornado pela fonte oficial. */
    @Test
    void shouldUpsertOfficialPricingByModelCode() {
        OpenAiPricingPageClient client = mock(OpenAiPricingPageClient.class);
        OpenAiModelRepository repository = mock(OpenAiModelRepository.class);
        OpenAiModel existing = new OpenAiModel();
        when(client.fetchAllModelPricing()).thenReturn(List.of(new OpenAiModelPricing(
                "gpt-5.5",
                "gpt-5.5",
                new BigDecimal("5.00"),
                new BigDecimal("0.50"),
                new BigDecimal("30.00"),
                new BigDecimal("2.50"),
                new BigDecimal("0.25"),
                new BigDecimal("15.00"))));
        when(repository.findByCode("gpt-5.5")).thenReturn(Optional.of(existing));

        int updated = new OpenAiModelPricingSyncService(client, repository).syncOfficialPricing();

        assertThat(updated).isEqualTo(1);
        assertThat(existing.getCode()).isEqualTo("gpt-5.5");
        assertThat(existing.getPriceInputStandard()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(existing.getPriceOutputBatch()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(existing.getPricingSource()).isEqualTo(OpenAiPricingPageClient.PRICING_PAGE_URL);
        assertThat(existing.getLastPricingSyncAt()).isNotNull();
        verify(repository).save(existing);
    }

    /** Garante que a sincronização persista preços de modelos de imagem quando publicados pela fonte oficial. */
    @Test
    void shouldUpsertImageModelPricingByModelCode() {
        OpenAiPricingPageClient client = mock(OpenAiPricingPageClient.class);
        OpenAiModelRepository repository = mock(OpenAiModelRepository.class);
        OpenAiModel existing = new OpenAiModel();
        when(client.fetchAllModelPricing()).thenReturn(List.of(new OpenAiModelPricing(
                "gpt-image-2",
                "gpt-image-2",
                new BigDecimal("8.00"),
                new BigDecimal("2.00"),
                new BigDecimal("30.00"),
                new BigDecimal("4.00"),
                new BigDecimal("1.00"),
                new BigDecimal("15.00"))));
        when(repository.findByCode("gpt-image-2")).thenReturn(Optional.of(existing));

        int updated = new OpenAiModelPricingSyncService(client, repository).syncOfficialPricing();

        assertThat(updated).isEqualTo(1);
        assertThat(existing.getCode()).isEqualTo("gpt-image-2");
        assertThat(existing.getPriceInputStandard()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(existing.getPriceInputCachedStandard()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(existing.getPriceOutputStandard()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(existing.getPriceInputBatch()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(existing.getPriceInputCachedBatch()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(existing.getPriceOutputBatch()).isEqualByComparingTo(new BigDecimal("15.00"));
        verify(repository).save(existing);
    }

}
