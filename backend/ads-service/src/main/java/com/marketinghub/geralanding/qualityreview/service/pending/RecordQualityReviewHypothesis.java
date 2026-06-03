package com.marketinghub.geralanding.qualityreview.service.pending;

import java.util.Map;
import java.util.UUID;

/** Representa a hipótese e o framework canônico usados pela revisão visual de qualidade. */
public record RecordQualityReviewHypothesis(
        UUID id,
        String title,
        Map<String, Object> framework
) {
    /** Mantém o contrato imutável da hipótese exposta para o Worker AI. */
    public RecordQualityReviewHypothesis {}
}
