package com.marketinghub.geralanding.designpreset.service;

import com.marketinghub.geralanding.GeraLandingStartResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import org.springframework.stereotype.Service;

/** Responsável por iniciar a execução da etapa de design preset do GeraLanding. */
@Service
public class GeraLandingDesignPresetStageService {
  private static final String STAGE_NAME = "landing-page-design-preset";
  private final GeraLandingStageExecutionService executionService;

  public GeraLandingDesignPresetStageService(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Inicia a execução da etapa de design preset para o experimento informado. */
  public GeraLandingDesignPresetStartResponse start(Long experimentId) {
    GeraLandingStartResponse response = executionService.registerInitialExecution(experimentId, STAGE_NAME);
    return new GeraLandingDesignPresetStartResponse(response.idJob(), response.status());
  }
}
