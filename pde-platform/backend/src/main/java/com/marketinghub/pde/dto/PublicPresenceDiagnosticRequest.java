package com.marketinghub.pde.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/** Recebe respostas do diagnóstico público de presença antes do acesso da cliente. */
public record PublicPresenceDiagnosticRequest(
        @NotEmpty Map<String, String> answers,
        String experienceVersion
) {
    /** Mantém compatibilidade com a degustação das versões anteriores do MUSA. */
    public PublicPresenceDiagnosticRequest(Map<String, String> answers) {
        this(answers, null);
    }
}
