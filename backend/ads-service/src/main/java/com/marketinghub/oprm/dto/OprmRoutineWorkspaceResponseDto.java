package com.marketinghub.oprm.dto;

import java.util.List;
import java.util.Map;

public record OprmRoutineWorkspaceResponseDto(
        String occupationSeedRef,
        String lastCorrelationId,
        Map<String, Object> routineCardPayload,
        Map<String, Object> frameworkInputPayload,
        List<Map<String, Object>> painSignals,
        List<Map<String, Object>> desiredOutcomeSignals,
        List<Map<String, Object>> mechanismOpportunitySignals
) {
}
