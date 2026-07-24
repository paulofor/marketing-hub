package com.marketinghub.experiment.monitoring.dto;

import com.marketinghub.pde.PdeProductionSlotStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados necessários para criar ou atualizar um slot produtivo versionado do PDE. */
public record PostDeployPdeProductionSlotRequestDto(
        @NotBlank @Size(max = 64) String slotCode,
        @NotBlank @Size(max = 191) String productSlug,
        @NotBlank @Size(max = 191) String domain,
        @Size(max = 512) String publicUrl,
        @Size(max = 512) String backendUrl,
        @NotBlank @Size(max = 120) String experienceVersion,
        @Size(max = 64) String targetEnvironment,
        PdeProductionSlotStatus status,
        Long sourceExperimentId,
        String notes
) {}
