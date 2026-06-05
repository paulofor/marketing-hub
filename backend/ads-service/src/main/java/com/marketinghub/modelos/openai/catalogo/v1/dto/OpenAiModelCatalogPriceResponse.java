package com.marketinghub.modelos.openai.catalogo.v1.dto;

import java.math.BigDecimal;

/** Responsabilidade: transportar os preços oficiais de um modelo listado no catálogo OpenAI. */
public record OpenAiModelCatalogPriceResponse(
        BigDecimal priceInputStandard,
        BigDecimal priceInputCachedStandard,
        BigDecimal priceOutputStandard,
        BigDecimal priceInputBatch,
        BigDecimal priceInputCachedBatch,
        BigDecimal priceOutputBatch) {}
