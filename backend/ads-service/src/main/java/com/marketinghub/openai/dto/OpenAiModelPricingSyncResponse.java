package com.marketinghub.openai.dto;

import java.time.Instant;

/** Responsabilidade: expor o resultado da sincronização manual de preços financeiros OpenAI. */
public record OpenAiModelPricingSyncResponse(int modelsUpdated, String source, Instant syncedAt) {}
