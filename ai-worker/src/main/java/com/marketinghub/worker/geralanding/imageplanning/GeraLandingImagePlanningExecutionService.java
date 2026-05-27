package com.marketinghub.worker.geralanding.imageplanning;

import com.marketinghub.worker.geralanding.GeraLandingExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa image planning usando o executor compartilhado. */
@Service
public class GeraLandingImagePlanningExecutionService {
    private final GeraLandingExecutionService executionService;

    public GeraLandingImagePlanningExecutionService(GeraLandingExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa os jobs pendentes da etapa image planning. */
    public void processExecutions(List<GeraLandingStageExecutionImagePlanningDto> jobs) {
        executionService.processExecutions(jobs.stream().map(GeraLandingStageExecutionImagePlanningDto::toBase).toList());
    }
}
