package com.marketinghub.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar parsing e resolução de preços retornados pela API autenticada OpenAI. */
class OpenAiPricingPageClientTest {

    /** Garante que a rotina aproveite preços quando a API autenticada da OpenAI publicar metadados financeiros. */
    @Test
    void shouldParsePricingMetadataFromAuthenticatedModelsApi() throws Exception {
        OpenAiPricingPageClient client = new OpenAiPricingPageClient();

        List<OpenAiModelPricing> prices = client.parseAuthenticatedApiPricing(new ObjectMapper().readTree("""
                {
                  "data": [
                    {
                      "id": "gpt-image-2",
                      "pricing": {
                        "standard": {
                          "image": {"input": 8, "cached_input": 2, "output": 30}
                        },
                        "batch": {
                          "image": {"input": 4, "cached_input": 1, "output": 15}
                        }
                      }
                    }
                  ]
                }
                """));

        assertThat(prices).hasSize(1);
        OpenAiModelPricing pricing = prices.get(0);
        assertThat(pricing.code()).isEqualTo("gpt-image-2");
        assertThat(pricing.priceInputStandard()).isEqualByComparingTo(new BigDecimal("8"));
        assertThat(pricing.priceInputCachedStandard()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(pricing.priceOutputStandard()).isEqualByComparingTo(new BigDecimal("30"));
        assertThat(pricing.priceInputBatch()).isEqualByComparingTo(new BigDecimal("4"));
        assertThat(pricing.priceInputCachedBatch()).isEqualByComparingTo(new BigDecimal("1"));
        assertThat(pricing.priceOutputBatch()).isEqualByComparingTo(new BigDecimal("15"));
    }

    /** Garante que ausência de batch não seja substituída por cálculo local. */
    @Test
    void shouldNotInventBatchPricingWhenApiDoesNotReturnBatch() throws Exception {
        OpenAiPricingPageClient client = new OpenAiPricingPageClient();

        List<OpenAiModelPricing> prices = client.parseAuthenticatedApiPricing(new ObjectMapper().readTree("""
                {
                  "data": [
                    {
                      "id": "gpt-image-2",
                      "pricing": {
                        "standard": {
                          "image": {"input": 8, "cached_input": 2, "output": 30}
                        }
                      }
                    }
                  ]
                }
                """));

        assertThat(prices).isEmpty();
    }

    /** Garante que uma execução sem cliente autenticado falhe em vez de usar qualquer fallback. */
    @Test
    void shouldFailWhenAuthenticatedClientIsMissing() {
        OpenAiPricingPageClient client = new OpenAiPricingPageClient();

        assertThatThrownBy(client::fetchAllModelPricing)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cliente autenticado da OpenAI não configurado");
    }

    /** Garante que variantes datadas usem o preço-base oficial mais específico. */
    @Test
    void shouldResolvePricingForDatedModelVariantUsingMostSpecificBaseCode() {
        OpenAiPricingPageClient client = new OpenAiPricingPageClient();
        List<OpenAiModelPricing> prices = List.of(
                new OpenAiModelPricing(
                        "gpt-5.4",
                        "gpt-5.4",
                        new BigDecimal("2.50"),
                        new BigDecimal("0.25"),
                        new BigDecimal("15.00"),
                        new BigDecimal("1.25"),
                        new BigDecimal("0.125"),
                        new BigDecimal("7.50")),
                new OpenAiModelPricing(
                        "gpt-5.4-pro",
                        "gpt-5.4-pro",
                        new BigDecimal("30.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("180.00"),
                        new BigDecimal("15.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("90.00")));

        OpenAiModelPricing pricing = client
                .findBestTextModelPricing(prices, "gpt-5.4-pro-2026-03-05")
                .orElseThrow();

        assertThat(pricing.code()).isEqualTo("gpt-5.4-pro");
        assertThat(pricing.priceInputStandard()).isEqualByComparingTo(new BigDecimal("30.00"));
    }
}
