package com.marketinghub.scientificresearch.productevidence.v1.pipeline;

/**
 * Define os códigos canônicos das etapas do pipeline de evidência científica.
 */
public enum StageCode {
    SOURCE_DISCOVERY("source-discovery"),
    EVIDENCE_SYNTHESIS("evidence-synthesis"),
    DELIVERABLE_COMPOSER("deliverable-composer");

    private final String code;

    StageCode(String code) {
        this.code = code;
    }

    /**
     * Retorna o código externo da etapa.
     */
    public String code() {
        return code;
    }
}
