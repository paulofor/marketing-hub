package com.marketinghub.agentmonitor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Responsabilidade: receber a prova segura de versão, backend e autenticação do executor. */
public record AgentExecutorHealthReportRequest(
    @NotBlank String agentKey,
    @NotNull Integer deployedVersion,
    String buildReference,
    @NotNull Boolean backendAccessible,
    @NotNull Boolean codexAuthenticated,
    String detail) {}
