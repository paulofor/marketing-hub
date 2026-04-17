package com.marketinghub.mds.search;

import java.util.List;

public record ScreenedEvidence(
        String source,
        String sourceDocumentId,
        String title,
        String abstractText,
        String doi,
        String url,
        String publicationYear,
        List<String> limitations,
        String proximityWithProblem,
        String applicabilityToNiche,
        List<String> evidenceStrengthSignals,
        double relevanceScore,
        double applicabilityScore,
        double priorityScore
) {
}
