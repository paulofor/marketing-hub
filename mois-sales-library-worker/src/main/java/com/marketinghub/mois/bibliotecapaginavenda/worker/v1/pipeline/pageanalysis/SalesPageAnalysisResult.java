package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis;

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
        String geralandingWireframeJson,
        String geralandingCopyJson,
        String geralandingImagePromptJson,
        String geralandingDesignPresetJson,
        String analysisNotes,
        String requestPayloadJson,
        String responsePayloadJson,
        String parserVersion,
        String promptVersion,
        String modelName,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal modelCostUsd
) {
}
