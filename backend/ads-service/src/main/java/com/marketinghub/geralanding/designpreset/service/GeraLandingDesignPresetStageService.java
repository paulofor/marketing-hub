package com.marketinghub.geralanding.designpreset.service;

import org.springframework.stereotype.Service;

/** Responsável por iniciar a execução da etapa de design preset do GeraLanding. */
@Service
public class GeraLandingDesignPresetStageService {
  private static final String STAGE_NAME = "landing-page-design-preset";
  private final GeraLandingDesignPresetStageExecutionService executionService;

  public GeraLandingDesignPresetStageService(GeraLandingDesignPresetStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Inicia a execução da etapa de design preset para o experimento informado. */
  public GeraLandingDesignPresetStartResponse start(Long experimentId) {
    var execution = executionService.registerInitialExecution(experimentId, STAGE_NAME);
    return new GeraLandingDesignPresetStartResponse(execution.idJob(), execution.status());
  }

}
