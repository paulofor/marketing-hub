package com.marketinghub.oprm.dto;

import java.util.List;
import java.util.Map;

public record OprmInsightsWorkspaceResponseDto(
        String occupationSeedRef,
        String lastCorrelationId,
        List<OprmArtifactSummaryDto> timeline,
        List<Map<String, Object>> sources,
        List<Map<String, Object>> excerpts,
        Map<String, Object> lineage,
        List<Map<String, Object>> feedbackSnapshots,
        Map<String, Object> feedbackComparison
) {
}
