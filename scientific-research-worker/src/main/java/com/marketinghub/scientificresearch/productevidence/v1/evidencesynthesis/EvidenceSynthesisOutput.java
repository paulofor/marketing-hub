package com.marketinghub.scientificresearch.productevidence.v1.evidencesynthesis;

import java.util.List;

/**
 * Estrutura a síntese científica aprovada ou bloqueada pela IA.
 */
public record EvidenceSynthesisOutput(
        boolean approvedForProductClaim,
        String scientificPrinciple,
        String plainLanguageExplanation,
        List<String> supportedClaims,
        List<String> forbiddenClaims,
        List<String> evidenceLimits,
        List<String> citedSources,
        String recommendedProductAngle) {
}
