package com.marketinghub.modelos.openai.catalogo.v1.dto;

import java.util.List;
import java.util.Map;

/** Responsabilidade: transportar modelos oficiais OpenAI e preços encontrados para seleção na tela. */
public record OpenAiModelCatalogResponse(
        List<String> textModels,
        List<String> imageModels,
        Map<String, OpenAiModelCatalogPriceResponse> pricingByModel,
        String source,
        String fetchedAt) {}
