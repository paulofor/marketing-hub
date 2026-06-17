package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis;

/** Entrada da etapa de análise comercial baseada no HTML capturado da página de vendas. */
public record PageAnalysisInput(
        long pageId,
        String urlCanonical,
        String title,
        String rawHtml
) {
}
