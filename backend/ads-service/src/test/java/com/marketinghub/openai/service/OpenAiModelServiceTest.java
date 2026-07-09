package com.marketinghub.openai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.modelos.openai.catalogo.v1.dto.OpenAiModelCatalogResponse;
import com.marketinghub.modelos.openai.catalogo.v1.service.OpenAiModelCatalogV1Service;
import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.OpenAiModelPricing;
import com.marketinghub.openai.OpenAiPricingPageClient;
import com.marketinghub.openai.dto.CreateOpenAiModelRequest;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a criação de modelos OpenAI com preenchimento por fontes oficiais. */
class OpenAiModelServiceTest {

    /** Garante que a criação use apenas nome e preencha dados oficiais de catálogo e preço. */
    @Test
    void createShouldUseOnlyNameAndFillOfficialOpenAiData() {
        OpenAiModelRepository repository = mock(OpenAiModelRepository.class);
        OpenAiModelCatalogV1Service catalogService = mock(OpenAiModelCatalogV1Service.class);
        OpenAiPricingPageClient pricingPageClient = mock(OpenAiPricingPageClient.class);
        OpenAiModelService service = new OpenAiModelService(repository, catalogService, pricingPageClient);
        CreateOpenAiModelRequest request = new CreateOpenAiModelRequest();
        request.setName("GPT 5.5");
        when(catalogService.fetchAndPersistCatalog()).thenReturn(new OpenAiModelCatalogResponse(
                List.of("gpt-5.5"),
                List.of("gpt-image-1"),
                Map.of(),
                "openai:/models",
                "2026-06-05T00:00:00Z"));
        OpenAiModelPricing pricing = new OpenAiModelPricing(
                "gpt-5.5",
                "gpt-5.5",
                new BigDecimal("5.00"),
                new BigDecimal("0.50"),
                new BigDecimal("30.00"),
                new BigDecimal("2.50"),
                new BigDecimal("0.25"),
                new BigDecimal("15.00"));
        List<OpenAiModelPricing> prices = List.of(pricing);
        when(pricingPageClient.fetchAllModelPricing()).thenReturn(prices);
        when(pricingPageClient.findBestModelPricing(prices, "gpt-5.5")).thenReturn(Optional.of(pricing));
        when(repository.findByCode("gpt-5.5")).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(OpenAiModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OpenAiModel created = service.create(request);

        assertThat(created.getCode()).isEqualTo("gpt-5.5");
        assertThat(created.getName()).isEqualTo("GPT 5.5");
        assertThat(created.getPriceInputStandard()).isEqualByComparingTo("5.00");
        assertThat(created.getPriceOutputBatch()).isEqualByComparingTo("15.00");
        assertThat(created.isAcceptsImageInput()).isFalse();
        assertThat(created.getPricingSource()).isEqualTo(OpenAiPricingPageClient.PRICING_PAGE_URL);
        assertThat(created.getLastPricingSyncAt()).isNotNull();
        verify(catalogService).fetchAndPersistCatalog();
    }

    /** Garante que a criação de variante datada use o preço-base oficial mais específico. */
    @Test
    void createShouldPriceDatedVariantFromMostSpecificBaseModel() {
        OpenAiModelRepository repository = mock(OpenAiModelRepository.class);
        OpenAiModelCatalogV1Service catalogService = mock(OpenAiModelCatalogV1Service.class);
        OpenAiPricingPageClient pricingPageClient = mock(OpenAiPricingPageClient.class);
        OpenAiModelService service = new OpenAiModelService(repository, catalogService, pricingPageClient);
        CreateOpenAiModelRequest request = new CreateOpenAiModelRequest();
        request.setName("gpt-5.4-pro-2026-03-05");
        when(catalogService.fetchAndPersistCatalog()).thenReturn(new OpenAiModelCatalogResponse(
                List.of("gpt-5.4-pro-2026-03-05"),
                List.of(),
                Map.of(),
                "openai:/models",
                "2026-06-05T00:00:00Z"));
        OpenAiModelPricing pricing = new OpenAiModelPricing(
                "gpt-5.4-pro",
                "gpt-5.4-pro",
                new BigDecimal("30.00"),
                BigDecimal.ZERO,
                new BigDecimal("180.00"),
                new BigDecimal("15.00"),
                BigDecimal.ZERO,
                new BigDecimal("90.00"));
        List<OpenAiModelPricing> prices = List.of(pricing);
        when(pricingPageClient.fetchAllModelPricing()).thenReturn(prices);
        when(pricingPageClient.findBestModelPricing(prices, "gpt-5.4-pro-2026-03-05"))
                .thenReturn(Optional.of(pricing));
        when(repository.findByCode("gpt-5.4-pro-2026-03-05")).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(OpenAiModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OpenAiModel created = service.create(request);

        assertThat(created.getCode()).isEqualTo("gpt-5.4-pro-2026-03-05");
        assertThat(created.getPriceInputStandard()).isEqualByComparingTo("30.00");
        assertThat(created.getPriceOutputBatch()).isEqualByComparingTo("90.00");
    }

    /** Garante que a criação bloqueie modelos inexistentes na API oficial /models. */
    @Test
    void createShouldRejectNameMissingFromOfficialCatalog() {
        OpenAiModelRepository repository = mock(OpenAiModelRepository.class);
        OpenAiModelCatalogV1Service catalogService = mock(OpenAiModelCatalogV1Service.class);
        OpenAiPricingPageClient pricingPageClient = mock(OpenAiPricingPageClient.class);
        OpenAiModelService service = new OpenAiModelService(repository, catalogService, pricingPageClient);
        CreateOpenAiModelRequest request = new CreateOpenAiModelRequest();
        request.setName("modelo inexistente");
        when(catalogService.fetchAndPersistCatalog()).thenReturn(new OpenAiModelCatalogResponse(
                List.of("gpt-5.5"), List.of(), Map.of(), "openai:/models", "2026-06-05T00:00:00Z"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Modelo OpenAI não encontrado");
    }
}
