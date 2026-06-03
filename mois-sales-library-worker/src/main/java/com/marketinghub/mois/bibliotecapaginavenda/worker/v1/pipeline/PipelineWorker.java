package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/** Orquestra uma etapa de pipeline sem conhecer tecnologia concreta ou detalhes da etapa. */
@Slf4j
public class PipelineWorker<I, O> {

    private final StageBackendPort<I, O> backendPort;
    private final StageProcessor<I, O> processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o worker genérico com portas e processador plugáveis. */
    public PipelineWorker(StageBackendPort<I, O> backendPort, StageProcessor<I, O> processor, ArtifactStore artifactStore) {
        this.backendPort = Objects.requireNonNull(backendPort);
        this.processor = Objects.requireNonNull(processor);
        this.artifactStore = Objects.requireNonNull(artifactStore);
    }

    /** Processa uma execução pendente, quando houver, e retorna se algum item foi executado. */
    public boolean processNext() {
        StageExecution<I> execution = backendPort.claimNext();
        if (execution == null) {
            return false;
        }
        try {
            StageResult<O> result = processor.process(new StageContext<>(execution, execution.input(), artifactStore));
            backendPort.markCompleted(execution, result);
            return true;
        } catch (Exception ex) {
            log.warn("Falha ao executar etapa genérica de pipeline. stageCode={}, executionId={}, erroClasse={}, erro={}",
                    execution.stageCode(), execution.idJob(), ex.getClass().getName(), ex.getMessage(), ex);
            backendPort.markFailed(execution, ex);
            return true;
        }
    }
}
