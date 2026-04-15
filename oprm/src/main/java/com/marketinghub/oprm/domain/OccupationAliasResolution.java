package com.marketinghub.oprm.domain;

public record OccupationAliasResolution(
        String rawLabel,
        String normalizedLabel,
        String matchedOccupationName,
        String matchType,
        double matchConfidence,
        String resolverNotes) {
}
