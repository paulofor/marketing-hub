package com.marketinghub.worker.geralanding.wireframe.monitor;

import com.marketinghub.worker.geralanding.wireframe.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.wireframe.request.GeraLandingWireframeOpenAiExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Processa os jobs pendentes da etapa wireframe com o executor OpenAI da própria etapa. */
@Service
public class WireframeExecutionProcessor {
    private final GeraLandingWireframeOpenAiExecutionService executionService;

    public WireframeExecutionProcessor(GeraLandingWireframeOpenAiExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa a lista de jobs pendentes retornada pelo polling da etapa wireframe. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) {
        executionService.processExecutions(jobs);
    }
}
