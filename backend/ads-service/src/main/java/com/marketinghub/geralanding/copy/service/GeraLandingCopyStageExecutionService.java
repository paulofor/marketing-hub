package com.marketinghub.geralanding.copy.service;

import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import org.springframework.stereotype.Service;

/** Responsável por encapsular a execução de etapas do domínio de copy do GeraLanding. */
@Service
public class GeraLandingCopyStageExecutionService {
  private final GeraLandingStageExecutionService stageExecutionService;

  public GeraLandingCopyStageExecutionService(GeraLandingStageExecutionService stageExecutionService) {
    this.stageExecutionService = stageExecutionService;
  }

  /** Registra a execução inicial da etapa de copy e devolve os dados para acompanhamento. */
  public GeraLandingCopyStartResponse registerInitialExecution(Long experimentId, String stageCode) {
    var response = stageExecutionService.registerInitialExecution(experimentId, stageCode);
    return new GeraLandingCopyStartResponse(response.idJob(), response.status());
  }
}
