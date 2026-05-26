package com.marketinghub.geralanding.designpreset;

import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetStageExecutionService;
import java.nio.charset.StandardCharsets;
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
    return new GeraLandingDesignPresetStartResponse(fromDatabaseIdJob(execution.getIdJob()), execution.getStatus());
  }


  /** Converte o id do formato persistido para o formato textual da API. */
  private String fromDatabaseIdJob(byte[] idJob) {
    return new String(idJob, StandardCharsets.UTF_8);
  }
}
