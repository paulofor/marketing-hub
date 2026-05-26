package com.marketinghub.geralanding.copy.service;

import com.marketinghub.geralanding.GeraLandingStartResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import org.springframework.stereotype.Service;

/** Responsável por iniciar a execução da etapa de copy do GeraLanding. */
@Service
public class GeraLandingCopyStageService {
  private static final String STAGE_NAME = "landing-page-copy";
  private final GeraLandingStageExecutionService executionService;

  public GeraLandingCopyStageService(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Inicia a execução da etapa de copy para o experimento informado. */
  public GeraLandingCopyStartResponse start(Long experimentId) {
    GeraLandingStartResponse response = executionService.registerInitialExecution(experimentId, STAGE_NAME);
    return new GeraLandingCopyStartResponse(response.idJob(), response.status());
  }
}
