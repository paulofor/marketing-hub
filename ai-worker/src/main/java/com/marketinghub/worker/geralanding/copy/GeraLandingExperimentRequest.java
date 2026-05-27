package com.marketinghub.worker.geralanding.copy;

import java.util.Map;

/** Responsabilidade: concentrar os dados do experimento para montagem de request da etapa copy. */
public record GeraLandingExperimentRequest(
        Long experimentId,
        Map<String, Object> dados
) {
    /** Retorna fallback textual quando valor estiver nulo ou em branco. */
    public String resolveText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
