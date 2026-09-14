package com.marketinghub.product.executionprofile.v1.service.saveprofile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** Responsabilidade: solicitar uma nova revisão sem modificar fichas ou execuções anteriores. */
public record SaveProfileRequest(
    @NotNull Long chainId,
    @NotNull Long commercialPlanId,
    @NotBlank @Size(max = 100) String createdBy,
    @NotNull @Valid ProfileContract contract) {}
