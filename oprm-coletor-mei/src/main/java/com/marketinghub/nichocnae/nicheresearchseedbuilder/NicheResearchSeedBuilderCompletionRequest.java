package com.marketinghub.nichocnae.nicheresearchseedbuilder;

/** Envia ao backend a saída validada da etapa dois junto com metadados mínimos de auditoria da IA. */
public record NicheResearchSeedBuilderCompletionRequest(
        NicheResearchSeedBuilderOutput output,
        String model,
        String rawModelResponse,
        String rawOpenAiRequest,
        String rawOpenAiResponse,
        Integer inputTokens,
        Integer outputTokens,
        String openAiResponseId) {}
