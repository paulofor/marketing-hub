package com.marketinghub.openai;

import java.math.BigDecimal;

/** Responsabilidade: transportar os preços oficiais por 1 milhão de tokens de um modelo OpenAI. */
public record OpenAiModelPricing(
        String code,
        String name,
        BigDecimal priceInputStandard,
        BigDecimal priceInputCachedStandard,
        BigDecimal priceOutputStandard,
        BigDecimal priceInputBatch,
        BigDecimal priceInputCachedBatch,
        BigDecimal priceOutputBatch) {}
