package com.marketinghub.feo.fabricacaov1.pipeline;

/**
 * Enumera as etapas canonicas da FEO v1.
 */
public enum StageCode {
    PLANEJAMENTO_ENTREGAVEIS("planejamento-entregaveis"),
    REDACAO_ENTREGAVEIS("redacao-entregaveis"),
    MONTAGEM_PACOTE("montagem-pacote");

    private final String code;

    /**
     * Guarda o codigo usado nos contratos com o backend.
     */
    StageCode(String code) {
        this.code = code;
    }

    /**
     * Retorna o codigo canonico da etapa.
     */
    public String code() {
        return code;
    }
}
