package com.marketinghub.worker.openai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

/**
 * Estimates the approximate cost of an OpenAI Responses API call using
 * published prices per 1K tokens.
 */
public final class OpenAiCostEstimator {
    private static final BigDecimal ONE_THOUSAND = BigDecimal.valueOf(1000);

    private static final Map<String, Pricing> PRICING_BY_MODEL = Map.ofEntries(
            Map.entry("gpt-3.5-turbo", new Pricing(new BigDecimal("0.0005"), new BigDecimal("0.0015"))),
            Map.entry("gpt-4o", new Pricing(new BigDecimal("0.005"), new BigDecimal("0.015"))),
            Map.entry("gpt-4o-mini", new Pricing(new BigDecimal("0.00015"), new BigDecimal("0.0006"))),
            Map.entry("o1-mini", new Pricing(new BigDecimal("0.003"), new BigDecimal("0.012"))),
            Map.entry("o1-preview", new Pricing(new BigDecimal("0.015"), new BigDecimal("0.060")))
    );

    private static final Pricing DEFAULT_PRICING = PRICING_BY_MODEL.get("gpt-3.5-turbo");

    private OpenAiCostEstimator() {
    }

    public static BigDecimal estimateUsd(String model, OpenAiResponse.OpenAiUsage usage) {
        if (usage == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        Pricing pricing = resolvePricing(model);
        int inputTokens = usage.effectiveInputTokens() != null ? usage.effectiveInputTokens() : 0;
        int outputTokens = usage.effectiveOutputTokens() != null ? usage.effectiveOutputTokens() : 0;
        BigDecimal inputCost = pricing.inputCostPer1k()
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(ONE_THOUSAND, 6, RoundingMode.HALF_UP);
        BigDecimal outputCost = pricing.outputCostPer1k()
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(ONE_THOUSAND, 6, RoundingMode.HALF_UP);
        return inputCost.add(outputCost).setScale(4, RoundingMode.HALF_UP);
    }

    private static Pricing resolvePricing(String model) {
        if (model == null || model.isBlank()) {
            return DEFAULT_PRICING;
        }
        String normalised = model.toLowerCase(Locale.ROOT);
        return PRICING_BY_MODEL.getOrDefault(normalised, DEFAULT_PRICING);
    }

    private record Pricing(BigDecimal inputCostPer1k, BigDecimal outputCostPer1k) {
    }
}
