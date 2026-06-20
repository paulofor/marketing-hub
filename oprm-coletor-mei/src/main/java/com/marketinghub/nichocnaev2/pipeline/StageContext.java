package com.marketinghub.nichocnaev2.pipeline;

import java.util.Map;
import java.util.stream.Collectors;

/** Representa o contexto genérico recebido por uma etapa executora do pipeline NichoCNAE versão 2. */
public record StageContext(String jobId, String stageExecutionId, Map<String, Object> input) {
    /** Cria um contexto com mapa de entrada nunca nulo e sem metadados nulos vindos do backend. */
    public StageContext {
        input = input == null ? Map.of() : withoutNullValues(input);
    }

    /** Remove valores nulos antes de congelar o mapa, pois Map.copyOf não aceita null. */
    private static Map<String, Object> withoutNullValues(Map<String, Object> input) {
        return input.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
