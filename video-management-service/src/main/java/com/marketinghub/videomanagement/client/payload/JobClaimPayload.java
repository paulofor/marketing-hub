package com.marketinghub.videomanagement.client.payload;

/** Declara capacidade do executor para recuperar voz e compor capturas ou amostras narradas. */
public record JobClaimPayload(String workerId, String message, String postProductionContract) {
    public static final String RECOVERY_CONTRACT = "PDE_VIDEO_POST_PRODUCTION_V3";

    /** Mantém chamadas existentes declarando a capacidade implementada por esta versão. */
    public JobClaimPayload(String workerId, String message) {
        this(workerId, message, RECOVERY_CONTRACT);
    }
}
