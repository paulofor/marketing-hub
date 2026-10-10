package com.marketinghub.videomanagement.client.payload;

/** Declara identidade do executor e o contrato de recuperação que ele sabe executar. */
public record JobClaimPayload(String workerId, String message, String postProductionContract) {
    public static final String RECOVERY_CONTRACT = "PRESERVED_TTS_NARRATION_V1";

    /** Mantém chamadas existentes declarando a capacidade implementada por esta versão. */
    public JobClaimPayload(String workerId, String message) {
        this(workerId, message, RECOVERY_CONTRACT);
    }
}
