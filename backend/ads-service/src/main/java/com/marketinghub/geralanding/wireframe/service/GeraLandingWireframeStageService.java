package com.marketinghub.geralanding.wireframe.service;

import org.springframework.stereotype.Service;

/** Responsável por iniciar a execução da etapa de wireframe do GeraLanding. */
@Service
public class GeraLandingWireframeStageService {
  private static final String STAGE_NAME = "landing-page-wireframe";
  private final GeraLandingWireframeStageExecutionService executionService;

  public GeraLandingWireframeStageService(GeraLandingWireframeStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Inicia a execução da etapa de wireframe para o experimento informado. */
  public GeraLandingWireframeStartResponse start(Long experimentId) {
    var execution = executionService.registerInitialExecution(experimentId, STAGE_NAME);
    return new GeraLandingWireframeStartResponse(execution.idJob(), execution.status());
  }

}
