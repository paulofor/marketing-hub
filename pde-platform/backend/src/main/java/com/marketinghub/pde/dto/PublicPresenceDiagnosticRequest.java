package com.marketinghub.pde.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/** Recebe respostas do diagnóstico público de presença antes do acesso da cliente. */
public record PublicPresenceDiagnosticRequest(
        @NotEmpty Map<String, String> answers
) {}
