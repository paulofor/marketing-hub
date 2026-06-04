package com.marketinghub.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class OpenAiPricingPageClientTest {

    @Test
    void shouldParseStandardAndBatchPricesFromOfficialPricingTables() {
        OpenAiProperties properties = new OpenAiProperties();
        OpenAiPricingPageClient client = new OpenAiPricingPageClient(WebClient.builder(), properties);

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

    @Test
    void shouldUseHalfStandardAsBatchFallbackWhenBatchTableIsMissing() {
        OpenAiProperties properties = new OpenAiProperties();
        OpenAiPricingPageClient client = new OpenAiPricingPageClient(WebClient.builder(), properties);

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
}
