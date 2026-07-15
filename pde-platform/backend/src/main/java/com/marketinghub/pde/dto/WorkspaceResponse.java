package com.marketinghub.pde.dto;

import java.util.List;

/** Retorna o produto e o progresso da cliente na área PDE. */
public record WorkspaceResponse(
        ProductExperienceResponse product,
        String email,
        int completedMissions,
        int totalMissions,
        int progressPercent,
        List<String> completedMissionIds
) {}
