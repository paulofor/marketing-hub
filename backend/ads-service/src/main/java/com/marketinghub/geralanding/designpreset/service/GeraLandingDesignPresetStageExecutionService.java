package com.marketinghub.geralanding.designpreset.service;

import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import org.springframework.stereotype.Service;

/** Responsável por encapsular a execução de etapas do domínio de design preset do GeraLanding. */
@Service
public class GeraLandingDesignPresetStageExecutionService {
  private final GeraLandingStageExecutionService stageExecutionService;

  public GeraLandingDesignPresetStageExecutionService(
      GeraLandingStageExecutionService stageExecutionService) {
    this.stageExecutionService = stageExecutionService;
  }

  /** Registra a execução inicial da etapa de design preset e devolve os dados para acompanhamento. */
  public GeraLandingDesignPresetStartResponse registerInitialExecution(Long experimentId, String stageCode) {
    var response = stageExecutionService.registerInitialExecution(experimentId, stageCode);
    return new GeraLandingDesignPresetStartResponse(response.idJob(), response.status());
  }
}
