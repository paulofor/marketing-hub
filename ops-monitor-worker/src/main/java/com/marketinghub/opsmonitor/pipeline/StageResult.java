package com.marketinghub.opsmonitor.pipeline;

import java.util.List;

/** Padroniza o resultado funcional e auditável de uma etapa. */
public record StageResult(boolean success, String status, String message, List<StageArtifact> artifacts) {

    /** Cria um resultado sem artefatos para cenários simples. */
    public static StageResult of(boolean success, String status, String message) {
        return new StageResult(success, status, message, List.of());
    }
}
