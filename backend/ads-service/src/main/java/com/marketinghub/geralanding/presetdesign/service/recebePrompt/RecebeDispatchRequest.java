package com.marketinghub.geralanding.presetdesign.service.recebePrompt;

/** Representa o payload interno que confirma o job remoto da IA para a etapa presetdesign. */
public record RecebeDispatchRequest(
        Long experimentId,
        String stageCode,
        String openAiJobId) {}
