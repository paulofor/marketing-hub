package com.marketinghub.modelos.openai.catalogo.v1.dto;

import java.util.List;

public record OpenAiModelCatalogResponse(List<String> textModels, List<String> imageModels, String source, String fetchedAt) {}
