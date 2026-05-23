package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.openai;

import java.math.BigDecimal;

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
        String modelName
) {
}
