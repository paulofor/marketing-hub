package com.marketinghub.worker.geralanding.wireframe.monitor;

import com.marketinghub.worker.geralanding.wireframe.openai.GeraLandingWireframeOpenAiExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa wireframe usando executor da própria etapa. */
@Service("geraLandingWireframeExecutionStageService")
public class GeraLandingExecutionWireframeService {
    private final GeraLandingWireframeOpenAiExecutionService executionService;
    public GeraLandingExecutionWireframeService(GeraLandingWireframeOpenAiExecutionService executionService) { this.executionService = executionService; }
    /** Processa os jobs pendentes da etapa wireframe. */
    public void processExecutions(List<GeraLandingStageExecutionWireframeDto> jobs) { executionService.processExecutions(jobs); }
}
