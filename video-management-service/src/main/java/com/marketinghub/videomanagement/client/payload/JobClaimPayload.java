package com.marketinghub.videomanagement.client.payload;

/** Declara capacidade do executor para recuperar voz e compor capturas e amostras narradas na mesma montagem. */
public record JobClaimPayload(String workerId, String message, String postProductionContract) {
    public static final String RECOVERY_CONTRACT = "PDE_VIDEO_POST_PRODUCTION_V4";

    /** Mantém chamadas existentes declarando a capacidade implementada por esta versão. */
    public JobClaimPayload(String workerId, String message) {
        this(workerId, message, RECOVERY_CONTRACT);
    }
}
