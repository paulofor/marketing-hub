package com.marketinghub.geralanding.copy;

import com.marketinghub.geralanding.copy.service.GeraLandingCopyStageExecutionService;
import java.nio.charset.StandardCharsets;
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
    return new GeraLandingCopyStartResponse(fromDatabaseIdJob(execution.getIdJob()), execution.getStatus());
  }


  /** Converte o id do formato persistido para o formato textual da API. */
  private String fromDatabaseIdJob(byte[] idJob) {
    return new String(idJob, StandardCharsets.UTF_8);
  }
}
