package com.marketinghub.worker.geralanding.presetdesign.response;

import java.math.BigDecimal;

/** Responsabilidade: representar o payload de resposta da OpenAI para conclusão da etapa presetdesign. */
public record RecordPresetDesignResponse(
        String responseContent,
        String rawResponse,
        String requestBodyJson,
        String openAiJobId,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {}
