package com.marketinghub.opsmonitor.pipeline;

import java.time.Instant;
import java.util.Map;

/** Mantém o contexto genérico de uma execução de etapa do monitoramento. */
public record StageContext(String stageExecutionId, String moduleCode, Instant requestedAt, Map<String, Object> metadata) {

    /** Cria um contexto vazio para testes e execuções simples. */
    public static StageContext simple(String stageExecutionId, String moduleCode) {
        return new StageContext(stageExecutionId, moduleCode, Instant.now(), Map.of());
    }
}
