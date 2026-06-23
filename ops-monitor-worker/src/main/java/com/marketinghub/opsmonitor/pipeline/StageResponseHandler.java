package com.marketinghub.opsmonitor.pipeline;

/** Centraliza o tratamento genérico de respostas produzidas pelas etapas. */
public class StageResponseHandler {

    /** Converte uma resposta funcional em resultado padronizado do pipeline. */
    public StageResult handleSuccess(String status, String message) {
        return StageResult.of(true, status, message);
    }
}
