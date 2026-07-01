package com.marketinghub.worker.openai.core.openai;

import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Responsabilidade: calcular custo de uso OpenAI com preços obtidos do catálogo persistido no backend. */
public class OpenAiCostEstimator {

    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

    private final OpenAiModelPricingCatalogClient pricingCatalogClient;

    /** Inicializa o estimador com o cliente que consulta o catálogo de preços persistido no banco pelo backend. */
    public OpenAiCostEstimator(OpenAiModelPricingCatalogClient pricingCatalogClient) {
        this.pricingCatalogClient = Objects.requireNonNull(pricingCatalogClient, "pricingCatalogClient must not be null");
    }

    /** Estima o custo usando o modelo efetivo do request e os preços batch/flex cadastrados no banco. */
    public BigDecimal estimate(String model, Integer inputTokens, Integer outputTokens) {
        return estimate(model, inputTokens, outputTokens, "flex");
    }

    /** Estima o custo usando o modo de preço correspondente ao service tier da etapa. */
    public BigDecimal estimate(String model, Integer inputTokens, Integer outputTokens, String serviceTier) {
        OpenAiModelPricingCatalogClient.OpenAiModelPricing pricing = pricingCatalogClient.findByCode(model)
                .orElseThrow(() -> new StageWorkerException(
                        "Modelo OpenAI não encontrado no catálogo persistido para cálculo de custo: " + model));
        return estimateWithPricing(pricing, inputTokens, outputTokens, serviceTier);
    }

    /** Aplica a tarifa por milhão de tokens aos totais de entrada e saída retornados pela OpenAI. */
    private BigDecimal estimateWithPricing(
            OpenAiModelPricingCatalogClient.OpenAiModelPricing pricing,
            Integer inputTokens,
            Integer outputTokens,
            String serviceTier
    ) {
        boolean standard = "default".equalsIgnoreCase(serviceTier) || "standard".equalsIgnoreCase(serviceTier);
        BigDecimal input = BigDecimal.valueOf(inputTokens == null ? 0 : inputTokens)
                .multiply(nullToZero(standard ? pricing.priceInputStandard() : pricing.priceInputBatch()))
                .divide(ONE_MILLION, 8, RoundingMode.HALF_UP);

        BigDecimal output = BigDecimal.valueOf(outputTokens == null ? 0 : outputTokens)
                .multiply(nullToZero(standard ? pricing.priceOutputStandard() : pricing.priceOutputBatch()))
                .divide(ONE_MILLION, 8, RoundingMode.HALF_UP);

        return input.add(output).setScale(8, RoundingMode.HALF_UP);
    }

    /** Normaliza preços ausentes no catálogo para zero sem mascarar a ausência do modelo. */
    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
