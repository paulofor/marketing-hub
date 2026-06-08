package com.marketinghub.worker.pipeline;

import java.util.List;

/** Responsabilidade: abstrair a comunicação do worker genérico com o backend dono do estado da etapa. */
public interface StageBackendPort<I, O> {
    /** Lista execuções pendentes para processamento. */
    List<StageExecution<I>> listPending(int limit);

    /** Marca uma execução como em processamento antes da etapa chamar integrações externas. */
    void markRunning(StageExecution<I> execution);

    /** Persiste a conclusão bem-sucedida da etapa. */
    void markCompleted(StageExecution<I> execution, StageResult<O> result);

    /** Persiste a falha da etapa com contexto suficiente para diagnóstico. */
    void markFailed(StageExecution<I> execution, Throwable error);
}
