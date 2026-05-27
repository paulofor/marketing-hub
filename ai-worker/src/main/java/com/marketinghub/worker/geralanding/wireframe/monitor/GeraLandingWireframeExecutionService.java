package com.marketinghub.worker.geralanding.wireframe.monitor;

import com.marketinghub.worker.geralanding.wireframe.dto.GeraLandingStageExecutionWireframeDto;
import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa wireframe usando o executor compartilhado. */
@Service
@Deprecated
public class GeraLandingWireframeExecutionService {
    private final GeraLandingExecutionWireframeService executionService;

    public GeraLandingWireframeExecutionService(GeraLandingExecutionWireframeService executionService) {
        this.executionService = executionService;
    }

    /** Processa os jobs pendentes da etapa wireframe. */
    public void processExecutions(List<GeraLandingStageExecutionWireframeDto> jobs) {
        executionService.processExecutions(jobs);
    }
}
