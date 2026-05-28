package com.marketinghub.worker.geralanding.imageplanning;

import com.marketinghub.worker.geralanding.imageplanning.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.imageplanning.request.GeraLandingImagePlanningOpenAiExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa image planning usando executor da própria etapa. */
@Service
public class GeraLandingImagePlanningExecutionService {
    private final GeraLandingImagePlanningOpenAiExecutionService executionService;
    public GeraLandingImagePlanningExecutionService(GeraLandingImagePlanningOpenAiExecutionService executionService) { this.executionService = executionService; }
    /** Processa os jobs pendentes da etapa image planning. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) { executionService.processExecutions(jobs); }
}
