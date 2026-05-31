package com.marketinghub.geralanding.copy.service;

import org.springframework.stereotype.Service;

/** Responsável por iniciar a execução da etapa de copy do GeraLanding. */
@Service
public class GeraLandingCopyStageService {
  private final GeraLandingCopyStageExecutionService executionService;

  /** Inicializa o serviço de fachada com o executor da etapa copy. */
  public GeraLandingCopyStageService(GeraLandingCopyStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Inicia a execução da etapa de copy para o experimento informado. */
  public GeraLandingCopyStartResponse start(Long experimentId) {
    return executionService.start(experimentId);
  }
}
