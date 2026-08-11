package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Responsabilidade: validar a decisão de um gate executada pelo agente responsável. */
public record DecideAgentGateRequest(
    @NotBlank @Size(max = 100) String decidedByAgentKey,
    @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String decision,
    @NotBlank String reason) {}
