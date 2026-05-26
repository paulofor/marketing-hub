package com.marketinghub.geralanding.deliverables.service;

import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import org.springframework.stereotype.Service;

/** Responsável por encapsular a execução de etapas do domínio de entregáveis do GeraLanding. */
@Service
public class GeraLandingDeliverablesStageExecutionService {
  private final GeraLandingStageExecutionService stageExecutionService;

  public GeraLandingDeliverablesStageExecutionService(
      GeraLandingStageExecutionService stageExecutionService) {
    this.stageExecutionService = stageExecutionService;
  }

  /** Registra a execução inicial da etapa de entregáveis e devolve os dados para acompanhamento. */
  public GeraLandingDeliverablesStartResponse registerInitialExecution(Long experimentId, String stageCode) {
    var response = stageExecutionService.registerInitialExecution(experimentId, stageCode);
    return new GeraLandingDeliverablesStartResponse(response.idJob(), response.status());
  }
}
