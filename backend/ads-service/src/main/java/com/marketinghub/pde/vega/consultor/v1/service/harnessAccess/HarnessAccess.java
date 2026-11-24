package com.marketinghub.pde.vega.consultor.v1.service.harnessAccess;

import jakarta.validation.constraints.*;

/** Identifica a tentativa sintética e o compartimento de cenário sem aceitar outro proprietário. */
public record HarnessAccess(
    @NotNull Long productId,
    @NotNull Long experimentId,
    @NotBlank String prototypeVersion,
    @NotBlank String sourceReference,
    @Pattern(regexp = "[A-Za-z0-9-]{16,80}") @NotNull String attemptId,
    @Pattern(regexp = "ADHERENT|RECOVERY|SAFETY") @NotNull String scenarioCode,
    @Pattern(regexp = "DESKTOP_1440|IPHONE_15_PRO|PIXEL_7") @NotNull String deviceProfile,
    @Pattern(regexp = "PRIMARY|ISOLATION") @NotNull String role,
    @Pattern(regexp = "[a-f0-9]{64}") @NotNull String recoveryKey) {}
