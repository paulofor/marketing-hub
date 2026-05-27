package com.marketinghub.geralanding.imageplanning.web;

import com.marketinghub.geralanding.imageplanning.service.GeraLandingImagePlanningExecutionSummaryResponse;
import com.marketinghub.geralanding.imageplanning.service.GeraLandingImagePlanningStageExecutionDetailResponse;
import com.marketinghub.geralanding.imageplanning.service.GeraLandingImagePlanningStageExecutionService;
import com.marketinghub.geralanding.imageplanning.service.GeraLandingImagePlanningStageService;
import com.marketinghub.geralanding.imageplanning.service.GeraLandingImagePlanningStartResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-image-planning no GeraLanding e expor consultas das execuções da etapa. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingImagePlanningController {
  private static final String STAGE_CODE = "landing-page-image-planning";

  private final GeraLandingImagePlanningStageService stageService;
  private final GeraLandingImagePlanningStageExecutionService executionService;

  public GeraLandingImagePlanningController(GeraLandingImagePlanningStageService stageService, GeraLandingImagePlanningStageExecutionService executionService) {
    this.stageService = stageService;
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-image-planning. */
  @PostMapping("/image-prompts/start")
  public ResponseEntity<GeraLandingImagePlanningStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingImagePlanningStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }

  /** Lista as execuções da etapa para o experimento. */
  @GetMapping("/image-prompts/stage-executions")
  public ResponseEntity<List<GeraLandingImagePlanningExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingImagePlanningExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, STAGE_CODE, includeCompleted);
    return ResponseEntity.ok(response);
  }

  /** Retorna os detalhes de uma execução específica da etapa. */
  @GetMapping("/image-prompts/stage-executions/{idJob}")
  public ResponseEntity<GeraLandingImagePlanningStageExecutionDetailResponse> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    GeraLandingImagePlanningStageExecutionDetailResponse response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
