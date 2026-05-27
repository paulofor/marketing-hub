package com.marketinghub.worker.geralanding.deliverables;

import com.marketinghub.worker.geralanding.deliverables.dto.GeraLandingStageExecutionDeliverablesDto;
import com.marketinghub.worker.geralanding.deliverables.request.GeraLandingDeliverablesOpenAiExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa deliverables usando executor da própria etapa. */
@Service
public class GeraLandingDeliverablesExecutionService {
    private final GeraLandingDeliverablesOpenAiExecutionService executionService;
    public GeraLandingDeliverablesExecutionService(GeraLandingDeliverablesOpenAiExecutionService executionService) { this.executionService = executionService; }
    /** Processa os jobs pendentes da etapa deliverables. */
    public void processExecutions(List<GeraLandingStageExecutionDeliverablesDto> jobs) { executionService.processExecutions(jobs); }
}
