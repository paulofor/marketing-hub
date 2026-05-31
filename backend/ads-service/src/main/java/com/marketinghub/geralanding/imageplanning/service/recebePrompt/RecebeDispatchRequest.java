package com.marketinghub.geralanding.imageplanning.service.recebePrompt;

/** Representa o payload interno com o identificador remoto do despacho na OpenAI. */
public record RecebeDispatchRequest(
        Long experimentId,
        String stageCode,
        String openAiJobId) {}
