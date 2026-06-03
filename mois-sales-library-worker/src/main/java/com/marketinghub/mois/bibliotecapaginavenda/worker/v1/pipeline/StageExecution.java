package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline;

import java.util.Map;

/** Representa uma execução reservada pelo backend para uma etapa genérica de pipeline. */
public record StageExecution<I>(
        long idJob,
        String stageCode,
        I input,
        Map<String, Object> config
) {
}
