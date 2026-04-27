package com.marketinghub.mois.dto;

import java.time.Instant;
import java.util.List;

public final class MoisAutomationDtos {

    private MoisAutomationDtos() {
    }

    public record HotmartRobotRunResponse(
            String runId,
            String status,
            String triggerType,
            String workspaceId,
            String niche,
            String marketTheme,
            String collectionJobId,
            int minSuccessScore,
            int limitPerSource,
            Instant triggeredAt,
            String errorMessage
    ) {
    }

    public record HotmartRobotRunListResponse(List<HotmartRobotRunResponse> items) {
    }
}
