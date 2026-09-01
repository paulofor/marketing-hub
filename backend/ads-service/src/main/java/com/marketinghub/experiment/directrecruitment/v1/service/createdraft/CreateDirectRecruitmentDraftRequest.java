package com.marketinghub.experiment.directrecruitment.v1.service.createdraft;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Responsabilidade: identificar quem solicitou a preparação do convite auditável. */
public record CreateDirectRecruitmentDraftRequest(@NotBlank @Size(max = 100) String createdBy) {}
