package com.marketinghub.nichocnae.pipeline;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Executa uma etapa do pipeline nichocnae usando apenas contratos genéricos de contexto, processor e resultado. */
public class PipelineWorker<I, O> {
    private static final Logger log = LoggerFactory.getLogger(PipelineWorker.class);

    private final StageProcessor<I, O> processor;
    private final ArtifactStore artifactStore;

    /** Inicializa o worker genérico com o processor concreto da etapa e armazenamento de artefatos. */
    public PipelineWorker(StageProcessor<I, O> processor, ArtifactStore artifactStore) {
        this.processor = Objects.requireNonNull(processor, "processor must not be null");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
    }

    /** Executa uma unidade de trabalho e registra falha operacional antes de devolver resultado negativo. */
    public StageWorkerResult process(StageExecution<I> execution) {
        Objects.requireNonNull(execution, "execution must not be null");
        try {
            processResult(execution);
            return StageWorkerResult.success(execution.idJob());
        } catch (RuntimeException ex) {
            log.error("Erro ao executar etapa do pipeline nichocnae (idJob={})", execution.idJob(), ex);
            return StageWorkerResult.failure(execution.idJob(), ex);
        }
    }

    /** Executa uma unidade de trabalho e devolve o resultado completo produzido pela etapa concreta. */
    public StageResult<O> processResult(StageExecution<I> execution) {
        Objects.requireNonNull(execution, "execution must not be null");
        StageContext<I> context = new StageContext<>(execution, execution.input(), artifactStore, execution.config());
        return processor.process(context);
    }
}
