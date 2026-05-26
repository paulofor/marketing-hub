package com.marketinghub.geralanding.imageplanning;

import com.marketinghub.geralanding.imageplanning.service.GeraLandingImagePlanningStageExecutionService;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

/** Responsável por iniciar a execução da etapa de planejamento de imagens do GeraLanding. */
@Service
public class GeraLandingImagePlanningStageService {
  private static final String STAGE_NAME = "landing-page-image-planning";
  private final GeraLandingImagePlanningStageExecutionService executionService;

  public GeraLandingImagePlanningStageService(GeraLandingImagePlanningStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Inicia a execução da etapa de planejamento de imagens para o experimento informado. */
  public GeraLandingImagePlanningStartResponse start(Long experimentId) {
    var execution = executionService.registerInitialExecution(experimentId, STAGE_NAME);
    return new GeraLandingImagePlanningStartResponse(fromDatabaseIdJob(execution.getIdJob()), execution.getStatus());
  }


  /** Converte o id do formato persistido para o formato textual da API. */
  private String fromDatabaseIdJob(byte[] idJob) {
    return new String(idJob, StandardCharsets.UTF_8);
  }
}
