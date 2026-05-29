package com.marketinghub.worker.openai.core.openai;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OpenAiCostEstimator {

    private final BigDecimal inputUsdPerMillionTokens;
    private final BigDecimal outputUsdPerMillionTokens;

    public OpenAiCostEstimator(
            BigDecimal inputUsdPerMillionTokens,
            BigDecimal outputUsdPerMillionTokens
    ) {
        this.inputUsdPerMillionTokens = inputUsdPerMillionTokens == null ? BigDecimal.ZERO : inputUsdPerMillionTokens;
        this.outputUsdPerMillionTokens = outputUsdPerMillionTokens == null ? BigDecimal.ZERO : outputUsdPerMillionTokens;
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
}
