package com.marketinghub.worker.geralanding.deliverables;

import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa deliverables usando o executor compartilhado. */
@Service
public class GeraLandingDeliverablesExecutionService {
    private final com.marketinghub.worker.geralanding.comum.GeraLandingExecutionService executionService;

    public GeraLandingDeliverablesExecutionService(com.marketinghub.worker.geralanding.comum.GeraLandingExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa os jobs pendentes da etapa deliverables. */
    public void processExecutions(List<GeraLandingStageExecutionDeliverablesDto> jobs) {
        executionService.processExecutions(jobs.stream()
                .map(item -> new com.marketinghub.worker.geralanding.comum.GeraLandingStageExecutionRef(item.experimentId(), item.idJob(), item.stageCode()))
                .toList());
    }
}
