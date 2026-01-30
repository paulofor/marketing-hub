package com.marketinghub.worker.targetingrequest;

import java.math.BigDecimal;

public record TargetingCandidateSuggestion(
        String textoSugerido,
        TargetingCandidateType tipo,
        String origem,
        BigDecimal score,
        String rationale,
        String idioma,
        String intentTag
) {
}
