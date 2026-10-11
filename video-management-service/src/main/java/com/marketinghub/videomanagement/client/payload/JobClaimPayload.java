package com.marketinghub.videomanagement.client.payload;

/** Declara identidade do executor e o contrato de recuperação e sequência narrada que ele sabe executar. */
public record JobClaimPayload(String workerId, String message, String postProductionContract) {
    public static final String RECOVERY_CONTRACT = "PDE_VIDEO_POST_PRODUCTION_V2";

    /** Mantém chamadas existentes declarando a capacidade implementada por esta versão. */
    public JobClaimPayload(String workerId, String message) {
        this(workerId, message, RECOVERY_CONTRACT);
    }
}
