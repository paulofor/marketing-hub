package com.marketinghub.openai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Valida cálculo de custo OpenAI conforme o modo de preço usado. */
class OpenAiPricingServiceTest {

  private final OpenAiModelRepository repository =
      org.mockito.Mockito.mock(OpenAiModelRepository.class);
  private final OpenAiPricingService service = new OpenAiPricingService(repository);

  /** Deve calcular Flex pelas tarifas Batch/Flex do catálogo. */
  @Test
  void estimateFlexCostShouldUseBatchPrices() {
    when(repository.findByCode("gpt-test")).thenReturn(Optional.of(model()));

    BigDecimal cost = service.estimateFlexCost("gpt-test", 1_000_000, 1_000_000);

    assertThat(cost).isEqualByComparingTo("8.7500");
  }

  /** Deve calcular Standard pelas tarifas Standard do catálogo. */
  @Test
  void estimateStandardCostShouldUseStandardPrices() {
    when(repository.findByCode("gpt-test")).thenReturn(Optional.of(model()));

    BigDecimal cost =
        service.estimateStandardCost(
            "gpt-test", new OpenAiResponse.OpenAiUsage(1_000_000, 1_000_000, null, null, null));

    assertThat(cost).isEqualByComparingTo("17.5000");
  }

  /** Deve descontar a parcela em cache da entrada comum e aplicar sua tarifa própria. */
  @Test
  void estimateTaskCostShouldSeparateCachedInput() {
    when(repository.findByCode("gpt-test")).thenReturn(Optional.of(model()));

    BigDecimal cost =
        service.estimateTaskCost("gpt-test", "FLEX", 1_000_000L, 600_000L, 200_000L).orElseThrow();

    assertThat(cost).isEqualByComparingTo("2.15000000");
  }

  /** Deve deixar o custo indisponível sem inventar tarifa para modelo desconhecido. */
  @Test
  void estimateTaskCostShouldRemainUnavailableWithoutCatalogPrice() {
    when(repository.findByCode("missing-model")).thenReturn(Optional.empty());

    assertThat(service.estimateTaskCost("missing-model", "FLEX", 100L, 20L, 30L)).isEmpty();
  }

  /** Mantém precisão econômica para tarefas com poucos tokens. */
  @Test
  void estimateTaskCostShouldNotRoundSmallUsageToZero() {
    when(repository.findByCode("gpt-test")).thenReturn(Optional.of(model()));

    BigDecimal cost = service.estimateTaskCost("gpt-test", "FLEX", 1L, 0L, 0L).orElseThrow();

    assertThat(cost).isEqualByComparingTo("0.00000125");
  }

  /** Deve falhar claramente quando o modelo não existe no catálogo. */
  @Test
  void estimateCostFailsWhenModelIsMissingFromCatalog() {
    when(repository.findByCode("missing-model")).thenReturn(Optional.empty());

    IllegalStateException error =
        assertThrows(
            IllegalStateException.class,
            () ->
                service.estimateStandardCost(
                    "missing-model", new OpenAiResponse.OpenAiUsage(1000, 1000, null, null, 2000)));

    assertThat(error.getMessage()).contains("Modelo OpenAI não encontrado no catálogo");
  }

  /** Cria modelo com preços diferentes para provar a escolha do modo. */
  private OpenAiModel model() {
    return OpenAiModel.builder()
        .code("gpt-test")
        .name("GPT Test")
        .priceInputStandard(new BigDecimal("2.50"))
        .priceInputCachedStandard(new BigDecimal("0.50"))
        .priceOutputStandard(new BigDecimal("15.00"))
        .priceInputBatch(new BigDecimal("1.25"))
        .priceInputCachedBatch(new BigDecimal("0.25"))
        .priceOutputBatch(new BigDecimal("7.50"))
        .build();
  }
}
