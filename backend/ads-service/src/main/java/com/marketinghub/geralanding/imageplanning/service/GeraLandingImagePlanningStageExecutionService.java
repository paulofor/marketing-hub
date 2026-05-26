package com.marketinghub.geralanding.imageplanning.service;

import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import org.springframework.stereotype.Service;

/** Responsável por encapsular a execução de etapas do domínio de planejamento de imagens do GeraLanding. */
@Service
public class GeraLandingImagePlanningStageExecutionService {
  private final GeraLandingStageExecutionService stageExecutionService;

  public GeraLandingImagePlanningStageExecutionService(
      GeraLandingStageExecutionService stageExecutionService) {
    this.stageExecutionService = stageExecutionService;
  }

  /** Registra a execução inicial da etapa de image planning e devolve os dados para acompanhamento. */
  public GeraLandingImagePlanningStartResponse registerInitialExecution(Long experimentId, String stageCode) {
    var response = stageExecutionService.registerInitialExecution(experimentId, stageCode);
    return new GeraLandingImagePlanningStartResponse(response.idJob(), response.status());
  }
}
