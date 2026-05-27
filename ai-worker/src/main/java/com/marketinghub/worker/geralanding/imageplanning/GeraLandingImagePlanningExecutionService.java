package com.marketinghub.worker.geralanding.imageplanning;

import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa image planning usando o executor compartilhado. */
@Service
public class GeraLandingImagePlanningExecutionService {
    private final com.marketinghub.worker.geralanding.comum.GeraLandingStageExecutionProcessor executionProcessor;

    public GeraLandingImagePlanningExecutionService(com.marketinghub.worker.geralanding.comum.GeraLandingStageExecutionProcessor executionProcessor) {
        this.executionProcessor = executionProcessor;
    }

    /** Processa os jobs pendentes da etapa image planning. */
    public void processExecutions(List<GeraLandingStageExecutionImagePlanningDto> jobs) {
        executionProcessor.processExecutions(jobs.stream()
                .map(item -> new com.marketinghub.worker.geralanding.comum.GeraLandingStageExecutionRef(item.experimentId(), item.idJob(), item.stageCode()))
                .toList());
    }
}
