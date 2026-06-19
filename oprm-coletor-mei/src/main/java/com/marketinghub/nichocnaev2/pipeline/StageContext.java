package com.marketinghub.nichocnaev2.pipeline;

import java.util.Map;

/** Representa o contexto genérico recebido por uma etapa executora do pipeline NichoCNAE versão 2. */
public record StageContext(String jobId, String stageExecutionId, Map<String, Object> input) {
    /** Cria um contexto com mapa de entrada nunca nulo para uso uniforme pelos processors. */
    public StageContext {
        input = input == null ? Map.of() : Map.copyOf(input);
    }
}
