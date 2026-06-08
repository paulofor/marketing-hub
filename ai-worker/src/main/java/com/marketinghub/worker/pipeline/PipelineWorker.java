package com.marketinghub.worker.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Responsabilidade: orquestrar execuções de etapas sem conhecer tecnologias ou etapas concretas. */
public class PipelineWorker<I, O> {
    private final StageBackendPort<I, O> backendPort;
    private final StageProcessor<I, O> processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o worker genérico com port de backend, processor concreto e store de artefatos. */
    public PipelineWorker(StageBackendPort<I, O> backendPort, StageProcessor<I, O> processor, ArtifactStore artifactStore) {
        this.backendPort = Objects.requireNonNull(backendPort, "backendPort must not be null");
        this.processor = Objects.requireNonNull(processor, "processor must not be null");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
    }

    /** Processa até o limite de execuções pendentes retornadas pelo backend da etapa. */
    public ProcessingSummary processPending(int limit) {
        List<StageExecution<I>> pending = backendPort.listPending(limit);
        List<StageWorkerResult> results = new ArrayList<>();
        for (StageExecution<I> execution : pending) {
            results.add(process(execution));
        }
        return ProcessingSummary.from(results);
    }

    /** Processa uma execução individual, atualizando status de sucesso ou falha no backend. */
    public StageWorkerResult process(StageExecution<I> execution) {
        Objects.requireNonNull(execution, "execution must not be null");
        try {
            backendPort.markRunning(execution);
            StageContext<I> context = new StageContext<>(execution, execution.input(), artifactStore, execution.config());
            StageResult<O> result = processor.process(context);
            backendPort.markCompleted(execution, result);
            return StageWorkerResult.success(execution.idJob());
        } catch (Exception error) {
            backendPort.markFailed(execution, error);
            return StageWorkerResult.failure(execution.idJob(), error);
        }
    }
}
