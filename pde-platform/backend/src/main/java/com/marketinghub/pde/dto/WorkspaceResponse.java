package com.marketinghub.pde.dto;

import java.util.List;

/** Retorna o produto e o progresso da cliente na área PDE. */
public record WorkspaceResponse(
        ProductExperienceResponse product,
        String email,
        String accessSource,
        String subscriptionStatus,
        int completedMissions,
        int totalMissions,
        int progressPercent,
        List<String> completedMissionIds,
        List<MissionInteractionResponse> missionInteractions
) {}
