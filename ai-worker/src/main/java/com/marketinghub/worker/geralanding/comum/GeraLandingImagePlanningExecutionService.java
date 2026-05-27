package com.marketinghub.worker.geralanding.comum;

import com.marketinghub.worker.geralanding.GeraLandingExecutionService;
import com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto;
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
    public void processExecutions(List<GeraLandingStageExecutionDto> jobs) {
        executionService.processExecutions(jobs);
    }
}
