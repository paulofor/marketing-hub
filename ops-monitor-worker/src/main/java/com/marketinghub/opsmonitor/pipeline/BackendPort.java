package com.marketinghub.opsmonitor.pipeline;

import java.util.Optional;

/** Define a porta genérica para comunicação persistente com o backend principal. */
public interface BackendPort {

    /** Busca a próxima pendência canônica entregue pelo backend. */
    Optional<String> fetchPendingStageExecution();

    /** Registra um resultado estruturado de etapa no backend. */
    void reportStageResult(String moduleCode, StageResult result);
}
