package com.marketinghub.worker.geralanding.wireframe;

import com.marketinghub.worker.geralanding.GeraLandingExecutionService;
import com.marketinghub.worker.geralanding.GeraLandingJobDto;
import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa wireframe usando o executor compartilhado. */
@Service
public class GeraLandingWireframeExecutionService {
    private final GeraLandingExecutionService executionService;

    public GeraLandingWireframeExecutionService(GeraLandingExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa os jobs pendentes da etapa wireframe. */
    public void processExecutions(List<GeraLandingJobDto> jobs) {
        executionService.processExecutions(jobs);
    }
}
