package com.marketinghub.openai.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: transportar dados de modelo OpenAI para as telas administrativas e seletores. */
public record OpenAiModelDto(
        Long id,
        String name,
        String code,
        BigDecimal priceInputStandard,
        BigDecimal priceInputCachedStandard,
        BigDecimal priceOutputStandard,
        BigDecimal priceInputBatch,
        BigDecimal priceInputCachedBatch,
        BigDecimal priceOutputBatch,
        boolean acceptsImageInput,
        String pricingSource,
        Instant lastPricingSyncAt) {}
