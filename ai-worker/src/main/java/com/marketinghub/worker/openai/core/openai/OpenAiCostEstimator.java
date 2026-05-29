package com.marketinghub.worker.openai.core.openai;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estimador simples de custo.
 *
 * Ajuste os valores por 1M tokens conforme o modelo usado.
 */
public class OpenAiCostEstimator {

    private final BigDecimal inputUsdPerMillionTokens;
    private final BigDecimal outputUsdPerMillionTokens;

    public OpenAiCostEstimator(
            BigDecimal inputUsdPerMillionTokens,
            BigDecimal outputUsdPerMillionTokens
    ) {
        this.inputUsdPerMillionTokens = inputUsdPerMillionTokens;
        this.outputUsdPerMillionTokens = outputUsdPerMillionTokens;
    }

    public BigDecimal estimate(Integer inputTokens, Integer outputTokens) {
        BigDecimal input = BigDecimal.valueOf(inputTokens == null ? 0 : inputTokens)
                .multiply(inputUsdPerMillionTokens)
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        BigDecimal output = BigDecimal.valueOf(outputTokens == null ? 0 : outputTokens)
                .multiply(outputUsdPerMillionTokens)
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);

        return input.add(output).setScale(8, RoundingMode.HALF_UP);
    }

    public static OpenAiCostEstimator zero() {
        return new OpenAiCostEstimator(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
