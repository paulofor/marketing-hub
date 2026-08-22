package com.marketinghub.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Responsabilidade: validar parsing e resolução de preços retornados pela fonte oficial de pricing
 * OpenAI.
 */
class OpenAiPricingPageClientTest {

  /**
   * Garante que o Spring injete o construtor operacional com WebClient em vez do construtor
   * auxiliar de parser.
   */
  @Test
  void shouldMarkOperationalConstructorForSpringInjection() throws Exception {
    Constructor<OpenAiPricingPageClient> constructor =
        OpenAiPricingPageClient.class.getConstructor(WebClient.Builder.class);

    assertThat(constructor.isAnnotationPresent(Autowired.class)).isTrue();
  }

  /** Garante que a rotina extraia preços de texto publicados na página oficial de pricing. */
  @Test
  void shouldParseTextPricingFromOfficialPricingPage() {
    OpenAiPricingPageClient client = new OpenAiPricingPageClient();

    List<OpenAiModelPricing> prices =
        client.parsePricingPage(
            """
                <div data-content-switcher-pane="true" data-value="standard">
                  <table>
                    <thead><tr><th>Model</th><th>Input</th><th>Cached input</th><th>Output</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                    <tbody><tr><td>gpt-5.5 (&lt;272K context length)</td><td>$5.00</td><td>$0.50</td><td>$30.00</td><td>$10.00</td><td>$1.00</td><td>$45.00</td></tr></tbody>
                  </table>
                </div>
                <div data-content-switcher-pane="true" data-value="batch">
                  <table>
                    <thead><tr><th>Model</th><th>Input</th><th>Cached input</th><th>Output</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                    <tbody><tr><td>gpt-5.5</td><td>$2.50</td><td>$0.25</td><td>$15.00</td><td>$5.00</td><td>$0.50</td><td>$22.50</td></tr></tbody>
                  </table>
                </div>
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

  /** Garante que a rotina extraia preços de imagem usando a modalidade Image da página oficial. */
  @Test
  void shouldParseImagePricingFromOfficialPricingPage() {
    OpenAiPricingPageClient client = new OpenAiPricingPageClient();

    List<OpenAiModelPricing> prices =
        client.parsePricingPage(
            """
                <div data-content-switcher-pane="true" data-value="standard">
                  <table>
                    <thead><tr><th>Model</th><th>Modality</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                    <tbody>
                      <tr><td>gpt-image-2</td><td>Image</td><td>$8.00</td><td>$2.00</td><td>$30.00</td></tr>
                      <tr><td>gpt-image-2</td><td>Text</td><td>$5.00</td><td>$1.25</td><td>-</td></tr>
                    </tbody>
                  </table>
                </div>
                <div data-content-switcher-pane="true" data-value="batch">
                  <table>
                    <thead><tr><th>Model</th><th>Modality</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                    <tbody><tr><td>gpt-image-2</td><td>Image</td><td>$4.00</td><td>$1.00</td><td>$15.00</td></tr></tbody>
                  </table>
                </div>
                """);

    assertThat(prices).hasSize(1);
    OpenAiModelPricing pricing = prices.get(0);
    assertThat(pricing.code()).isEqualTo("gpt-image-2");
    assertThat(pricing.priceInputStandard()).isEqualByComparingTo(new BigDecimal("8.00"));
    assertThat(pricing.priceInputCachedStandard()).isEqualByComparingTo(new BigDecimal("2.00"));
    assertThat(pricing.priceOutputStandard()).isEqualByComparingTo(new BigDecimal("30.00"));
    assertThat(pricing.priceInputBatch()).isEqualByComparingTo(new BigDecimal("4.00"));
    assertThat(pricing.priceInputCachedBatch()).isEqualByComparingTo(new BigDecimal("1.00"));
    assertThat(pricing.priceOutputBatch()).isEqualByComparingTo(new BigDecimal("15.00"));
  }

  /** Garante que ausência de batch na página oficial não seja substituída por cálculo local. */
  @Test
  void shouldNotInventBatchPricingWhenOfficialPricingPageDoesNotReturnBatch() {
    OpenAiPricingPageClient client = new OpenAiPricingPageClient();

    List<OpenAiModelPricing> prices =
        client.parsePricingPage(
            """
                <div data-content-switcher-pane="true" data-value="standard">
                  <table>
                    <thead><tr><th>Model</th><th>Input</th><th>Cached input</th><th>Output</th></tr></thead>
                    <tbody><tr><td>gpt-5.5</td><td>$5.00</td><td>$0.50</td><td>$30.00</td></tr></tbody>
                  </table>
                </div>
                """);

    assertThat(prices).isEmpty();
  }

  /** Garante que a rotina leia linhas completas serializadas nos props Astro da página oficial. */
  @Test
  void shouldParseAstroPropsFromOfficialPricingPage() {
    OpenAiPricingPageClient client = new OpenAiPricingPageClient();

    List<OpenAiModelPricing> prices =
        client.parsePricingPage(
            """
                <div data-content-switcher-pane="true" data-value="standard">
                  <astro-island component-export="TextTokenPricingTables"
                    props='{"rows":[1,[[1,[[0,"gpt-5.2"],[0,1.75],[0,0.175],[0,14]]]]]}'>
                  </astro-island>
                </div>
                <div data-content-switcher-pane="true" data-value="batch">
                  <astro-island component-export="TextTokenPricingTables"
                    props='{"rows":[1,[[1,[[0,"gpt-5.2"],[0,0.875],[0,0.0875],[0,7]]]]]}'>
                  </astro-island>
                </div>
                """);

    assertThat(prices).hasSize(1);
    OpenAiModelPricing pricing = prices.get(0);
    assertThat(pricing.code()).isEqualTo("gpt-5.2");
    assertThat(pricing.priceInputStandard()).isEqualByComparingTo(new BigDecimal("1.75"));
    assertThat(pricing.priceOutputBatch()).isEqualByComparingTo(new BigDecimal("7"));
  }

  /**
   * Garante a leitura do formato atual da tabela, com cache write e preços separados por tamanho de
   * contexto.
   */
  @Test
  void shouldParseGpt56SolPricingWithCacheWriteAndLongContextColumns() {
    OpenAiPricingPageClient client = new OpenAiPricingPageClient();

    List<OpenAiModelPricing> prices =
        client.parsePricingPage(
            """
                <div data-content-switcher-pane="true" data-value="standard">
                  <table><tbody><tr>
                    <td>gpt-5.6-sol</td><td>$4.00</td><td>$0.40</td><td>$5.00</td><td>$20.00</td>
                    <td>$8.00</td><td>$0.80</td><td>$10.00</td><td>$30.00</td>
                  </tr></tbody></table>
                </div>
                <div data-content-switcher-pane="true" data-value="batch">
                  <table><tbody><tr>
                    <td>gpt-5.6-sol</td><td>$2.00</td><td>$0.20</td><td>$2.50</td><td>$10.00</td>
                    <td>$4.00</td><td>$0.40</td><td>$5.00</td><td>$15.00</td>
                  </tr></tbody></table>
                </div>
                """);

    assertThat(prices).hasSize(1);
    OpenAiModelPricing pricing = prices.getFirst();
    assertThat(pricing.code()).isEqualTo("gpt-5.6-sol");
    assertThat(pricing.priceInputStandard()).isEqualByComparingTo("4.00");
    assertThat(pricing.priceInputCachedStandard()).isEqualByComparingTo("0.40");
    assertThat(pricing.priceOutputStandard()).isEqualByComparingTo("20.00");
    assertThat(pricing.priceInputBatch()).isEqualByComparingTo("2.00");
    assertThat(pricing.priceInputCachedBatch()).isEqualByComparingTo("0.20");
    assertThat(pricing.priceOutputBatch()).isEqualByComparingTo("10.00");
  }

  /** Garante a leitura do payload Astro atual, que inclui cache write antes do output. */
  @Test
  void shouldParseGpt56SolPricingFromCurrentAstroProps() {
    OpenAiPricingPageClient client = new OpenAiPricingPageClient();

    List<OpenAiModelPricing> prices =
        client.parsePricingPage(
            """
                <div data-content-switcher-pane="true" data-value="standard">
                  <astro-island component-export="TextTokenPricingTables"
                    props='{"rows":[1,[[1,[[0,"gpt-5.6-sol"],[0,4],[0,0.4],[0,5],[0,20]]]]]}'>
                  </astro-island>
                </div>
                <div data-content-switcher-pane="true" data-value="batch">
                  <astro-island component-export="TextTokenPricingTables"
                    props='{"rows":[1,[[1,[[0,"gpt-5.6-sol"],[0,2],[0,0.2],[0,2.5],[0,10]]]]]}'>
                  </astro-island>
                </div>
                """);

    assertThat(prices).hasSize(1);
    assertThat(prices.getFirst().priceInputStandard()).isEqualByComparingTo("4");
    assertThat(prices.getFirst().priceOutputStandard()).isEqualByComparingTo("20");
    assertThat(prices.getFirst().priceInputBatch()).isEqualByComparingTo("2");
    assertThat(prices.getFirst().priceOutputBatch()).isEqualByComparingTo("10");
  }

  /** Garante falha explícita quando uma nova forma de linha tornar a sincronização parcial. */
  @Test
  void shouldRejectPartialPricingSyncWhenPublishedModelCannotBeParsed() {
    OpenAiPricingPageClient client = new OpenAiPricingPageClient();
    String html =
        """
            <div data-content-switcher-pane="true" data-value="standard">
              <table><tbody><tr><td>gpt-future</td><td>$1</td><td>$0.1</td><td>$1.25</td><td>$5</td><td>extra</td></tr></tbody></table>
            </div>
            <div data-content-switcher-pane="true" data-value="batch">
              <table><tbody><tr><td>gpt-future</td><td>$0.5</td><td>$0.05</td><td>$0.625</td><td>$2.5</td><td>extra</td></tr></tbody></table>
            </div>
            """;

    assertThatThrownBy(() -> client.parsePricingPage(html))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("gpt-future");
  }

  /**
   * Garante que a rotina preserve compatibilidade com metadados financeiros legados da API
   * autenticada.
   */
  @Test
  void shouldParsePricingMetadataFromAuthenticatedModelsApi() throws Exception {
    OpenAiPricingPageClient client = new OpenAiPricingPageClient();

    List<OpenAiModelPricing> prices =
        client.parseAuthenticatedApiPricing(
            new ObjectMapper()
                .readTree(
                    """
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

  /** Garante que ausência de batch na API legada não seja substituída por cálculo local. */
  @Test
  void shouldNotInventBatchPricingWhenApiDoesNotReturnBatch() throws Exception {
    OpenAiPricingPageClient client = new OpenAiPricingPageClient();

    List<OpenAiModelPricing> prices =
        client.parseAuthenticatedApiPricing(
            new ObjectMapper()
                .readTree(
                    """
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

  /** Garante que uma execução sem cliente público falhe em vez de usar qualquer fallback. */
  @Test
  void shouldFailWhenPricingPageClientIsMissing() {
    OpenAiPricingPageClient client = new OpenAiPricingPageClient();

    assertThatThrownBy(client::fetchAllModelPricing)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cliente público da página de preços OpenAI não configurado");
  }

  /** Garante que variantes datadas usem o preço-base oficial mais específico. */
  @Test
  void shouldResolvePricingForDatedModelVariantUsingMostSpecificBaseCode() {
    OpenAiPricingPageClient client = new OpenAiPricingPageClient();
    List<OpenAiModelPricing> prices =
        List.of(
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

    OpenAiModelPricing pricing =
        client.findBestTextModelPricing(prices, "gpt-5.4-pro-2026-03-05").orElseThrow();

    assertThat(pricing.code()).isEqualTo("gpt-5.4-pro");
    assertThat(pricing.priceInputStandard()).isEqualByComparingTo(new BigDecimal("30.00"));
  }
}
