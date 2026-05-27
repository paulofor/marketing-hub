package com.marketinghub.geralanding.deliverables.service;

import org.springframework.stereotype.Service;

/** Responsável por iniciar execuções da etapa landing-page-deliverables. */
@Service
public class GeraLandingDeliverablesStageService {
  private static final String STAGE_NAME = "landing-page-deliverables";

  private final GeraLandingDeliverablesStageExecutionService executionService;

  public GeraLandingDeliverablesStageService(GeraLandingDeliverablesStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Registra a execução inicial da etapa e retorna identificadores para acompanhamento. */
  public GeraLandingDeliverablesStartResponse start(Long experimentId) {
    var execution = executionService.registerInitialExecution(experimentId, STAGE_NAME);
    return new GeraLandingDeliverablesStartResponse(execution.idJob(), execution.status());
  }

}
