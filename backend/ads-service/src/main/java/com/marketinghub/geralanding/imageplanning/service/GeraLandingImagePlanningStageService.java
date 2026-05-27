package com.marketinghub.geralanding.imageplanning.service;

import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import org.springframework.stereotype.Service;

/** Responsável por iniciar a execução da etapa de planejamento de imagens do GeraLanding. */
@Service
public class GeraLandingImagePlanningStageService {
  private static final String STAGE_NAME = "landing-page-image-planning";
  private final GeraLandingStageExecutionService executionService;

  public GeraLandingImagePlanningStageService(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Inicia a execução da etapa de planejamento de imagens para o experimento informado. */
  public GeraLandingImagePlanningStartResponse start(Long experimentId) {
    var execution = executionService.registerInitialExecution(experimentId, STAGE_NAME);
    return new GeraLandingImagePlanningStartResponse(execution.idJob(), execution.status());
  }

}
