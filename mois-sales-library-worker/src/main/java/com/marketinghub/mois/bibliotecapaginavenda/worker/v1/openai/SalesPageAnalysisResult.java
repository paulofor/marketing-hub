package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.openai;

import java.math.BigDecimal;

/**
 * Representa o resultado estruturado da análise OpenAI de uma página de vendas, incluindo uso e custo do modelo.
 */
public record SalesPageAnalysisResult(
        BigDecimal scoreTotal,
        String sectionsJson,
        String copyJson,
        String visualJson,
        String imageJson,
        String analysisNotes,
        String requestPayloadJson,
        String parserVersion,
        String promptVersion,
        String modelName,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal modelCostUsd
) {
}
