package com.marketinghub.worker.geralanding.wireframe;

import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa wireframe usando o executor base. */
@Service
public class GeraLandingExecutionWireframeService {
    private final com.marketinghub.worker.geralanding.GeraLandingExecutionService executionService;

    public GeraLandingExecutionWireframeService(com.marketinghub.worker.geralanding.GeraLandingExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa os jobs pendentes já filtrados da etapa wireframe. */
    public void processExecutions(List<GeraLandingStageExecutionWireframeDto> jobs) {
        executionService.processExecutions(jobs.stream().map(GeraLandingStageExecutionWireframeDto::toBase).toList());
    }
}
