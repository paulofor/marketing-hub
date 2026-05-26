package com.marketinghub.geralanding.deliverables.service;

import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import com.marketinghub.geralanding.GeraLandingStartResponse;
import org.springframework.stereotype.Service;

/** Responsável por iniciar execuções da etapa landing-page-deliverables. */
@Service
public class GeraLandingDeliverablesStageService {
  private static final String STAGE_NAME = "landing-page-deliverables";

  private final GeraLandingStageExecutionService executionService;

  public GeraLandingDeliverablesStageService(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Registra a execução inicial da etapa e retorna identificadores para acompanhamento. */
  public GeraLandingDeliverablesStartResponse start(Long experimentId) {
    GeraLandingStartResponse response = executionService.registerInitialExecution(experimentId, STAGE_NAME);
    return new GeraLandingDeliverablesStartResponse(response.idJob(), response.status());
  }
}
