package com.marketinghub.epm.service.createExperimentDecision;

import com.marketinghub.epm.ExperimentFinancialDecisionType;
import jakarta.validation.constraints.*;
import java.time.Instant;

/** Dados para registrar uma decisão financeira sobre um experimento. */
public record CreateExperimentDecisionRequest(@NotNull ExperimentFinancialDecisionType decisionType, @NotBlank String reason, Instant decidedAt, String decidedBy) {}
