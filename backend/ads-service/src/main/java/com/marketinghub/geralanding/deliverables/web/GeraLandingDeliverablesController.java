package com.marketinghub.geralanding.deliverables.web;

import com.marketinghub.geralanding.GeraLandingExecutionSummaryResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionDetailResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import com.marketinghub.geralanding.deliverables.service.GeraLandingDeliverablesStageService;
import com.marketinghub.geralanding.deliverables.service.GeraLandingDeliverablesStartResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-deliverables no GeraLanding e expor consultas das execuções da etapa. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingDeliverablesController {
  private static final String STAGE_CODE = "landing-page-deliverables";

  private final GeraLandingDeliverablesStageService stageService;
  private final GeraLandingStageExecutionService executionService;

  public GeraLandingDeliverablesController(GeraLandingDeliverablesStageService stageService, GeraLandingStageExecutionService executionService) {
    this.stageService = stageService;
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-deliverables. */
  @PostMapping("/deliverables/start")
  public ResponseEntity<GeraLandingDeliverablesStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingDeliverablesStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }

  /** Lista as execuções da etapa para o experimento. */
  @GetMapping("/deliverables/stage-executions")
  public ResponseEntity<List<GeraLandingExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, STAGE_CODE, includeCompleted);
    return ResponseEntity.ok(response);
  }

  /** Retorna os detalhes de uma execução específica da etapa. */
  @GetMapping("/deliverables/stage-executions/{idJob}")
  public ResponseEntity<GeraLandingStageExecutionDetailResponse> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    GeraLandingStageExecutionDetailResponse response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
