package com.marketinghub.businessprocesschain.learningcycle.v1.service.planningAlignment;

import jakarta.validation.constraints.*;
import java.util.UUID;

/** Responsabilidade: confirmar a reconciliação da definição já registrada, sem aprovar gastos. */
public record PlanningAlignmentRequest(
    @NotNull UUID requestKey,
    @Min(0) long expectedRevision,
    @Min(1) int expectedBigIdeaRevision,
    @NotBlank @Size(max = 160) String operatorName,
    @NotBlank @Size(max = 2000) String reason) {}
