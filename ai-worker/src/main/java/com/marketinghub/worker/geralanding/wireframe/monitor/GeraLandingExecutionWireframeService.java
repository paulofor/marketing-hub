package com.marketinghub.worker.geralanding.wireframe.monitor;

import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa wireframe usando o executor base. */
@Service("geraLandingWireframeExecutionStageService")
public class GeraLandingExecutionWireframeService {
    private final com.marketinghub.worker.geralanding.comum.GeraLandingStageExecutionProcessor executionProcessor;

    public GeraLandingExecutionWireframeService(
            com.marketinghub.worker.geralanding.comum.GeraLandingStageExecutionProcessor executionProcessor) {
        this.executionProcessor = executionProcessor;
    }

    /** Processa os jobs pendentes já filtrados da etapa wireframe convertendo para o DTO base. */
    public void processExecutions(List<GeraLandingStageExecutionWireframeDto> jobs) {
        List<com.marketinghub.worker.geralanding.comum.GeraLandingStageExecutionRef> mapped = jobs.stream()
                .map(item -> new com.marketinghub.worker.geralanding.comum.GeraLandingStageExecutionRef(item.experimentId(), item.idJob(), item.stageCode()))
                .toList();
        executionProcessor.processExecutions(mapped);
    }
}
