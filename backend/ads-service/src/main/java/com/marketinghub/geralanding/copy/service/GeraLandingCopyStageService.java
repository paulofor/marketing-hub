package com.marketinghub.geralanding.copy.service;

import org.springframework.stereotype.Service;

/** Responsável por iniciar a execução da etapa de copy do GeraLanding. */
@Service
public class GeraLandingCopyStageService {
  private static final String STAGE_NAME = "landing-page-copy";
  private final GeraLandingCopyStageExecutionService executionService;

  public GeraLandingCopyStageService(GeraLandingCopyStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Inicia a execução da etapa de copy para o experimento informado. */
  public GeraLandingCopyStartResponse start(Long experimentId) {
    var execution = executionService.registerInitialExecution(experimentId, STAGE_NAME);
    return new GeraLandingCopyStartResponse(execution.idJob(), execution.status());
  }

}
