package com.marketinghub.geralanding.deliverables.service.recebePrompt;

/** Representa o callback interno que informa o início do processamento ou o identificador remoto no provedor de IA. */
public record RecebeDispatchRequest(
        Long experimentId,
        String stageCode,
        String openAiJobId) {}
