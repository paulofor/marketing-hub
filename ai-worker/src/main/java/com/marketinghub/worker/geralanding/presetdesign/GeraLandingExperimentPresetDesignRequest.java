package com.marketinghub.worker.geralanding.presetdesign;

import java.util.Map;

/** Concentra os dados do experimento usados na geração da etapa presetdesign. */
public record GeraLandingExperimentPresetDesignRequest(Long experimentId, Map<String, Object> dados) {

    /** Retorna fallback quando o texto estiver nulo ou em branco. */
    public String resolveText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
