package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.prepareAdjustment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Identifica somente a candidata corrigida, sem aceitar orçamento, janela ou parecer fictício. */
public record PrepareAdjustmentSuccessorRequest(
    @Min(0) long expectedRevision,
    @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9._-]{2,63}") String productVersion) {}
