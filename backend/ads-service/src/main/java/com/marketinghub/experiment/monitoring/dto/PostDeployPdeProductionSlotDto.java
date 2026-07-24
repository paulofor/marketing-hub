package com.marketinghub.experiment.monitoring.dto;

import com.marketinghub.pde.PdeProductionSlotStatus;
import java.time.Instant;

/** Resume um slot produtivo versionado de PDE para uso em campanha. */
public record PostDeployPdeProductionSlotDto(
        Long id,
        String slotCode,
        String productSlug,
        String domain,
        String publicUrl,
        String backendUrl,
        String experienceVersion,
        String targetEnvironment,
        PdeProductionSlotStatus status,
        Long sourceExperimentId,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}
