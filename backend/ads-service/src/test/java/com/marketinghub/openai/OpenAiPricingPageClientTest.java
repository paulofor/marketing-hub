package com.marketinghub.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar parsing e resolução de preços oficiais OpenAI para modelos tokenizados. */
class OpenAiPricingPageClientTest {

    /** Garante compatibilidade com tabelas legadas de preços standard e batch por modelo textual. */
    @Test
    void shouldParseStandardAndBatchPricesFromOfficialPricingTables() {
        OpenAiProperties properties = new OpenAiProperties();
        OpenAiPricingPageClient client = new OpenAiPricingPageClient(properties);

        List<OpenAiModelPricing> prices = client.parseTextModelPricing("""
                <html><body>
                  <table>
                    <thead><tr><th>Model</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                    <tbody>
                      <tr><td>gpt-5.5 (&lt;272K context length)</td><td>$5.00</td><td>$0.50</td><td>$30.00</td></tr>
                      <tr><td>gpt-image-1</td><td>$5.00</td><td>$1.25</td><td>-</td></tr>
                    </tbody>
                  </table>
                  <table>
                    <thead><tr><th>Model</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                    <tbody>
                      <tr><td>gpt-5.5 (&lt;272K context length)</td><td>$2.50</td><td>$0.25</td><td>$15.00</td></tr>
                    </tbody>
                  </table>
                </body></html>
                """);

        assertThat(prices).hasSize(1);
        OpenAiModelPricing pricing = prices.get(0);
        assertThat(pricing.code()).isEqualTo("gpt-5.5");
        assertThat(pricing.priceInputStandard()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(pricing.priceInputCachedStandard()).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(pricing.priceOutputStandard()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(pricing.priceInputBatch()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(pricing.priceInputCachedBatch()).isEqualByComparingTo(new BigDecimal("0.25"));
        assertThat(pricing.priceOutputBatch()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    /** Garante fallback de batch como metade do standard quando a fonte não publica uma tabela batch. */
    @Test
    void shouldUseHalfStandardAsBatchFallbackWhenBatchTableIsMissing() {
        OpenAiProperties properties = new OpenAiProperties();
        OpenAiPricingPageClient client = new OpenAiPricingPageClient(properties);

        List<OpenAiModelPricing> prices = client.parseTextModelPricing("""
                <table>
                  <thead><tr><th>Model</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                  <tbody><tr><td>o4-mini</td><td>$1.10</td><td>$0.275</td><td>$4.40</td></tr></tbody>
                </table>
                """);

        assertThat(prices).hasSize(1);
        OpenAiModelPricing pricing = prices.get(0);
        assertThat(pricing.code()).isEqualTo("o4-mini");
        assertThat(pricing.priceInputBatch()).isEqualByComparingTo(new BigDecimal("0.55"));
        assertThat(pricing.priceInputCachedBatch()).isEqualByComparingTo(new BigDecimal("0.1375"));
        assertThat(pricing.priceOutputBatch()).isEqualByComparingTo(new BigDecimal("2.20"));
    }

    /** Garante que tabelas com contexto curto e longo usem o preço operacional de contexto curto. */
    @Test
    void shouldParsePricesFromCurrentShortAndLongContextTable() {
        OpenAiProperties properties = new OpenAiProperties();
        OpenAiPricingPageClient client = new OpenAiPricingPageClient(properties);

        List<OpenAiModelPricing> prices = client.parseTextModelPricing("""
                <table>
                  <thead>
                    <tr><th></th><th colspan="3">Short context</th><th colspan="3">Long context</th></tr>
                    <tr><th>Model</th><th>Input</th><th>Cached input</th><th>Output</th><th>Input</th><th>Cached input</th><th>Output</th></tr>
                  </thead>
                  <tbody>
                    <tr><td>gpt-5.4</td><td>$2.50</td><td>$0.25</td><td>$15.00</td><td>$5.00</td><td>$0.50</td><td>$22.50</td></tr>
                    <tr><td>gpt-5.4-mini</td><td>$0.75</td><td>$0.075</td><td>$4.50</td><td>-</td><td>-</td><td>-</td></tr>
                  </tbody>
                </table>
                """);

        assertThat(prices).hasSize(2);
        OpenAiModelPricing pricing = prices.get(0);
        assertThat(pricing.code()).isEqualTo("gpt-5.4");
        assertThat(pricing.priceInputStandard()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(pricing.priceInputCachedStandard()).isEqualByComparingTo(new BigDecimal("0.25"));
        assertThat(pricing.priceOutputStandard()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(pricing.priceInputBatch()).isEqualByComparingTo(new BigDecimal("1.25"));
        assertThat(pricing.priceInputCachedBatch()).isEqualByComparingTo(new BigDecimal("0.125"));
        assertThat(pricing.priceOutputBatch()).isEqualByComparingTo(new BigDecimal("7.50"));
    }


    /** Garante que modelos de imagem sejam sincronizados pela modalidade Image nas tabelas oficiais. */
    @Test
    void shouldParseImageModelPricesFromOfficialPricingTablesWithModalityColumn() {
        OpenAiProperties properties = new OpenAiProperties();
        OpenAiPricingPageClient client = new OpenAiPricingPageClient(properties);

        List<OpenAiModelPricing> prices = client.parseAllModelPricing("""
                <html><body>
                  <section>
                    <h3>Image generation models</h3>
                    <p>Standard</p>
                    <table>
                      <thead><tr><th>Model</th><th>Modality</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                      <tbody>
                        <tr><td>gpt-image-2</td><td>Image</td><td>$8.00</td><td>$2.00</td><td>$30.00</td></tr>
                        <tr><td></td><td>Text</td><td>$5.00</td><td>$1.25</td><td>-</td></tr>
                        <tr><td>gpt-image-1.5</td><td>Image</td><td>$8.00</td><td>$2.00</td><td>$32.00</td></tr>
                        <tr><td></td><td>Text</td><td>$5.00</td><td>$1.25</td><td>$10.00</td></tr>
                      </tbody>
                    </table>
                    <p>Batch</p>
                    <table>
                      <thead><tr><th>Model</th><th>Modality</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                      <tbody>
                        <tr><td>gpt-image-2</td><td>Image</td><td>$4.00</td><td>$1.00</td><td>$15.00</td></tr>
                        <tr><td></td><td>Text</td><td>$2.50</td><td>$0.625</td><td>-</td></tr>
                        <tr><td>gpt-image-1.5</td><td>Image</td><td>$4.00</td><td>$1.00</td><td>$16.00</td></tr>
                        <tr><td></td><td>Text</td><td>$2.50</td><td>$0.63</td><td>$5.00</td></tr>
                      </tbody>
                    </table>
                  </section>
                </body></html>
                """);

        assertThat(prices).extracting(OpenAiModelPricing::code).contains("gpt-image-2", "gpt-image-1.5");
        OpenAiModelPricing gptImage2 = prices.stream()
                .filter(pricing -> pricing.code().equals("gpt-image-2"))
                .findFirst()
                .orElseThrow();
        assertThat(gptImage2.priceInputStandard()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(gptImage2.priceInputCachedStandard()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(gptImage2.priceOutputStandard()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(gptImage2.priceInputBatch()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(gptImage2.priceInputCachedBatch()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(gptImage2.priceOutputBatch()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    /** Garante que o parser percorra todas as seções de preço e não apenas o primeiro par de tabelas. */
    @Test
    void shouldParseMultiplePricingSectionsInsteadOfOnlyFirstPairOfTables() {
        OpenAiProperties properties = new OpenAiProperties();
        OpenAiPricingPageClient client = new OpenAiPricingPageClient(properties);

        List<OpenAiModelPricing> prices = client.parseAllModelPricing("""
                <html><body>
                  <h3>Flagship chat models</h3>
                  <p>Standard</p>
                  <table>
                    <thead><tr><th>Model</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                    <tbody><tr><td>gpt-5.5</td><td>$5.00</td><td>$0.50</td><td>$30.00</td></tr></tbody>
                  </table>
                  <p>Batch</p>
                  <table>
                    <thead><tr><th>Model</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                    <tbody><tr><td>gpt-5.5</td><td>$2.50</td><td>$0.25</td><td>$15.00</td></tr></tbody>
                  </table>
                  <h3>Image generation models</h3>
                  <p>Standard</p>
                  <table>
                    <thead><tr><th>Model</th><th>Modality</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                    <tbody><tr><td>gpt-image-1.5</td><td>Image</td><td>$8.00</td><td>$2.00</td><td>$32.00</td></tr></tbody>
                  </table>
                  <p>Batch</p>
                  <table>
                    <thead><tr><th>Model</th><th>Modality</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                    <tbody><tr><td>gpt-image-1.5</td><td>Image</td><td>$4.00</td><td>$1.00</td><td>$16.00</td></tr></tbody>
                  </table>
                </body></html>
                """);

        assertThat(prices).extracting(OpenAiModelPricing::code).containsExactly("gpt-5.5", "gpt-image-1.5");
        OpenAiModelPricing image = prices.get(1);
        assertThat(image.priceOutputStandard()).isEqualByComparingTo(new BigDecimal("32.00"));
        assertThat(image.priceOutputBatch()).isEqualByComparingTo(new BigDecimal("16.00"));
    }

    /** Garante que variantes datadas usem o preço-base oficial mais específico. */
    @Test
    void shouldResolvePricingForDatedModelVariantUsingMostSpecificBaseCode() {
        OpenAiProperties properties = new OpenAiProperties();
        OpenAiPricingPageClient client = new OpenAiPricingPageClient(properties);
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
