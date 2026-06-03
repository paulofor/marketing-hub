package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline;

/** Define como o núcleo reserva e sinaliza execuções ao backend dono dos dados. */
public interface StageBackendPort<I, O> {

    /** Reserva a próxima execução pendente para esta etapa. */
    StageExecution<I> claimNext();

    /** Registra no backend que a execução foi concluída com sucesso. */
    void markCompleted(StageExecution<I> execution, StageResult<O> result);

    /** Registra no backend que a execução falhou com contexto operacional. */
    void markFailed(StageExecution<I> execution, Exception error);
}
