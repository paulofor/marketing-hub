package com.marketinghub.nichocnae.nicheresearchseedbuilder;

/** Preserva a resposta da OpenAI já convertida para saída de domínio e seus metadados de consumo. */
public record OpenAiSeedBuilderResult(
        NicheResearchSeedBuilderOutput output,
        String rawModelResponse,
        String rawOpenAiRequest,
        String rawOpenAiResponse,
        Integer inputTokens,
        Integer outputTokens,
        String openAiResponseId,
        String model) {}
