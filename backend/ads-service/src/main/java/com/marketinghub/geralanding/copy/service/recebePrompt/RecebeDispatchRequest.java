package com.marketinghub.geralanding.copy.service.recebePrompt;

/** Representa o callback interno que informa o identificador do job no provedor de IA. */
public record RecebeDispatchRequest(
        Long experimentId,
        String stageCode,
        String openAiJobId) {}
