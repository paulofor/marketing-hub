package com.marketinghub.geralanding.designpreset.web;

import com.marketinghub.geralanding.GeraLandingExecutionSummaryResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionDetailResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetStageService;
import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetStartResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-design-preset no GeraLanding e expor consultas das execuções da etapa. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingDesignPresetController {
  private static final String STAGE_CODE = "landing-page-design-preset";

  private final GeraLandingDesignPresetStageService stageService;
  private final GeraLandingStageExecutionService executionService;

  public GeraLandingDesignPresetController(GeraLandingDesignPresetStageService stageService, GeraLandingStageExecutionService executionService) {
    this.stageService = stageService;
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-design-preset. */
  @PostMapping("/design-preset/start")
  public ResponseEntity<GeraLandingDesignPresetStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingDesignPresetStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }

  /** Lista as execuções da etapa para o experimento. */
  @GetMapping("/design-preset/stage-executions")
  public ResponseEntity<List<GeraLandingExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, STAGE_CODE, includeCompleted);
    return ResponseEntity.ok(response);
  }

  /** Retorna os detalhes de uma execução específica da etapa. */
  @GetMapping("/design-preset/stage-executions/{idJob}")
  public ResponseEntity<GeraLandingStageExecutionDetailResponse> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    GeraLandingStageExecutionDetailResponse response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
