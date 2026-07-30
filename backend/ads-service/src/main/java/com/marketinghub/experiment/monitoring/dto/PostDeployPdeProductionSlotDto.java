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
    String layoutKey,
    String targetEnvironment,
    PdeProductionSlotStatus status,
    Long sourceExperimentId,
    String notes,
    String draftExperienceJson,
    String publishedExperienceJson,
    String publishedBy,
    Instant publishedAt,
    String validationStatus,
    Instant validationCheckedAt,
    Integer validationHttpStatus,
    String validationSummary,
    String validationDetail,
    String validationContractSlug,
    String validationContractHealthPath,
    String validationResolvedUrl,
    Instant createdAt,
    Instant updatedAt) {}
