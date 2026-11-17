package com.marketinghub.businessprocess.automation.v1.service.commands;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Responsabilidade: registrar o motivo explícito da pausa sem conceder autorização financeira. */
public record ProcessRunPauseRequest(
    @Pattern(regexp = "OPERATOR|COST_RECONCILIATION") String reasonCode,
    @Size(max = 1000) String reason) {}
