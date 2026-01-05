package com.marketinghub.openai.dto;

import java.math.BigDecimal;

public record OpenAiModelDto(
        Long id,
        String name,
        String code,
        BigDecimal priceInputStandard,
        BigDecimal priceInputCachedStandard,
        BigDecimal priceOutputStandard,
        BigDecimal priceInputBatch,
        BigDecimal priceInputCachedBatch,
        BigDecimal priceOutputBatch) {}
