package com.marketinghub.worker.geralanding;

import java.util.Map;

/**
 * Responsabilidade: concentrar os dados do experimento necessários para montar um request da OpenAI no GeraLanding.
 */
public record GeraLandingExperimentRequest(
        Long experimentId,
        String prompt,
        Map<String, Object> schema,
        String model,
        String systemName,
        String systemMessage
) {

    /** Retorna um valor textual de fallback quando o campo informado estiver nulo ou em branco. */
    public String resolveText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
