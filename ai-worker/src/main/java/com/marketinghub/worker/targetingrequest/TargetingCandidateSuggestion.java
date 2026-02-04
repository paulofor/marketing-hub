package com.marketinghub.worker.targetingrequest;

import java.math.BigDecimal;
import java.util.List;

public record TargetingCandidateSuggestion(
        String seed,
        List<String> seedVariants,
        TargetingCandidateType tipo,
        String origem,
        BigDecimal score,
        String rationale,
        String idiomaHint,
        String intentTag,
        String countryConstraint
) {
}
