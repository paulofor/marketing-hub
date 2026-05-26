package com.marketinghub.geralanding.imageplanning.service;

import org.springframework.stereotype.Service;

/** Responsável por iniciar a execução da etapa de planejamento de imagens do GeraLanding. */
@Service
public class GeraLandingImagePlanningStageService {
  private static final String STAGE_NAME = "landing-page-image-planning";
  private final GeraLandingImagePlanningStageExecutionService executionService;

  public GeraLandingImagePlanningStageService(GeraLandingImagePlanningStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Inicia a execução da etapa de planejamento de imagens para o experimento informado. */
  public GeraLandingImagePlanningStartResponse start(Long experimentId) {
    return executionService.registerInitialExecution(experimentId, STAGE_NAME);
  }
}
