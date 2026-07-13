package com.marketinghub.scientificresearch.productevidence.v1.pipeline;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Orquestra a execução genérica das etapas sem conhecer detalhes concretos.
 */
@Component
public class PipelineWorker {

    private static final Logger log = LoggerFactory.getLogger(PipelineWorker.class);

    private final StageBackendPort backendPort;
    private final Map<StageCode, StageProcessor> processors;

    /**
     * Monta o catálogo de processors disponíveis no executor.
     */
    public PipelineWorker(StageBackendPort backendPort, List<StageProcessor> processors) {
        this.backendPort = backendPort;
        this.processors = new EnumMap<>(StageCode.class);
        for (StageProcessor processor : processors) {
            this.processors.put(processor.stageCode(), processor);
        }
    }

    /**
     * Executa um ciclo de polling para todas as etapas registradas.
     */
    public void pollOnce() {
        for (StageCode stageCode : StageCode.values()) {
            StageProcessor processor = processors.get(stageCode);
            if (processor == null) {
                log.warn("Etapa {} sem processor registrado", stageCode.code());
                continue;
            }
            for (StageContext context : backendPort.fetchPending(stageCode)) {
                processOne(processor, context);
            }
        }
    }

    /**
     * Executa uma única pendência e reporta resultado ou falha.
     */
    private void processOne(StageProcessor processor, StageContext context) {
        try {
            StageResult result = processor.process(context);
            backendPort.reportResult(context, result);
        } catch (Exception ex) {
            log.error(
                    "Falha técnica no scientific-research-worker stage={} jobId={} executionId={}",
                    processor.stageCode().code(),
                    context.jobId(),
                    context.executionId(),
                    ex);
            backendPort.reportFailure(context, ex);
        }
    }
}
