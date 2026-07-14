package com.marketinghub.feo.fabricacaov1.pipeline;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Orquestra etapas genericas da FEO sem conhecer detalhes de cada etapa concreta.
 */
@Component
public class PipelineWorker {

    private static final Logger log = LoggerFactory.getLogger(PipelineWorker.class);

    private final StageBackendPort backendPort;
    private final ArtifactStore artifactStore;
    private final Map<StageCode, StageProcessor<?, ?>> processors;

    /**
     * Monta o catalogo de processors registrados no worker.
     */
    public PipelineWorker(StageBackendPort backendPort, ArtifactStore artifactStore, List<StageProcessor<?, ?>> processors) {
        this.backendPort = backendPort;
        this.artifactStore = artifactStore;
        this.processors = new EnumMap<>(StageCode.class);
        for (StageProcessor<?, ?> processor : processors) {
            this.processors.put(processor.stageCode(), processor);
        }
    }

    /**
     * Executa um ciclo de polling para todas as etapas contratadas.
     */
    public void pollOnce(int limit) {
        for (StageCode stageCode : StageCode.values()) {
            StageProcessor<?, ?> processor = processors.get(stageCode);
            if (processor == null) {
                log.warn("Etapa FEO sem processor registrado stage={}", stageCode.code());
                continue;
            }
            for (StageExecution<?> execution : backendPort.fetchPending(stageCode, limit)) {
                processOne(processor, execution);
            }
        }
    }

    /**
     * Executa uma pendencia e publica resultado ou falha.
     */
    private <I, O> void processOne(StageProcessor<I, O> processor, StageExecution<?> rawExecution) {
        @SuppressWarnings("unchecked")
        StageExecution<I> execution = (StageExecution<I>) rawExecution;
        try {
            StageResult<O> result = processor.process(new StageContext<>(execution, execution.input(), artifactStore));
            backendPort.reportResult(execution, result);
        } catch (Exception ex) {
            log.error("Falha tecnica na FEO stage={} jobId={} executionId={}",
                    processor.stageCode().code(),
                    execution.jobId(),
                    execution.executionId(),
                    ex);
            backendPort.reportFailure(execution, ex);
        }
    }
}
