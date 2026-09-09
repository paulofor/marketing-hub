package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.result;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Responsabilidade: receber resposta bruta, falha e consumo da proposta comercial assistida. */
public record DecisionProposalResult(
    @NotBlank @Size(max = 36) String leaseToken,
    @Size(max = 64000) String rawResponse,
    @Size(max = 4000) String error,
    @Min(0) Long inputTokens,
    @Min(0) Long outputTokens,
    @DecimalMin("0") BigDecimal costUsd) {}
