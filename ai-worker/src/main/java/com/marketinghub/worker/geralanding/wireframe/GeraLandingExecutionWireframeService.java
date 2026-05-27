package com.marketinghub.worker.geralanding.wireframe;

import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa wireframe usando o executor base. */
@Service("geraLandingWireframeExecutionStageService")
public class GeraLandingExecutionWireframeService {
    private final com.marketinghub.worker.geralanding.GeraLandingExecutionService executionService;

    public GeraLandingExecutionWireframeService(
            com.marketinghub.worker.geralanding.GeraLandingExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa os jobs pendentes já filtrados da etapa wireframe convertendo para o DTO base. */
    public void processExecutions(List<GeraLandingStageExecutionWireframeDto> jobs) {
        List<com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto> mapped = jobs.stream()
                .map(item -> new com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto(
                        item.experimentId(), item.idJob(), item.stageCode()))
                .toList();
        executionService.processExecutions(mapped);
    }
}
