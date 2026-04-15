package com.marketinghub.oprm.domain;

import java.util.List;

public record OccupationWebSourceSnapshotPayload(
        String occupationName,
        String nicheName,
        String locale,
        OccupationSourcePolicyProfile sourcePolicyProfile,
        List<CapturedWebSource> capturedSources,
        List<String> semanticRoutineSignals,
        String enrichmentSummary) {
}
