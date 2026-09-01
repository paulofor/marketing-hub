package com.marketinghub.experiment.directrecruitment.v1.service.activate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Responsabilidade: registrar a aprovação humana explícita do convite antes da ativação. */
public record ActivateDirectRecruitmentRequest(
    @NotBlank @Size(max = 100) String approvedBy, @NotNull @AssertTrue Boolean approvalConfirmed) {}
