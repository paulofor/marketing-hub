package com.marketinghub.worker.geralanding.deliverables;

import com.marketinghub.worker.geralanding.GeraLandingExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa deliverables usando o executor compartilhado. */
@Service
public class GeraLandingDeliverablesExecutionService {
    private final GeraLandingExecutionService executionService;

    public GeraLandingDeliverablesExecutionService(GeraLandingExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa os jobs pendentes da etapa deliverables. */
    public void processExecutions(List<GeraLandingStageExecutionDeliverablesDto> jobs) {
        executionService.processExecutions(jobs.stream().map(GeraLandingStageExecutionDeliverablesDto::toBase).toList());
    }
}
