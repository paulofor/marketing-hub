package com.marketinghub.worker.geralanding;

/**
 * Responsabilidade: concentrar os dados mínimos do experimento para montagem do request da OpenAI no GeraLanding.
 */
public record GeraLandingExperimentRequest(
        Long experimentId,
        String prompt
) {

    /** Retorna um valor textual de fallback quando o campo informado estiver nulo ou em branco. */
    public String resolveText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
