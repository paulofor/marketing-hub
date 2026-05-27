package com.marketinghub.geralanding.copy.web;

import com.marketinghub.geralanding.copy.service.GeraLandingCopyExecutionSummaryResponse;
import com.marketinghub.geralanding.copy.service.GeraLandingCopyStageExecutionDetailResponse;
import com.marketinghub.geralanding.copy.service.GeraLandingCopyStageExecutionService;
import com.marketinghub.geralanding.copy.service.GeraLandingCopyStageService;
import com.marketinghub.geralanding.copy.service.GeraLandingCopyStartResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-copy no GeraLanding e expor consultas das execuções da etapa. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingCopyController {
  private static final String STAGE_CODE = "landing-page-copy";

  private final GeraLandingCopyStageService stageService;
  private final GeraLandingCopyStageExecutionService executionService;

  public GeraLandingCopyController(GeraLandingCopyStageService stageService, GeraLandingCopyStageExecutionService executionService) {
    this.stageService = stageService;
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-copy. */
  @PostMapping("/copy/start")
  public ResponseEntity<GeraLandingCopyStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingCopyStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }

  /** Lista as execuções da etapa para o experimento. */
  @GetMapping("/copy/stage-executions")
  public ResponseEntity<List<GeraLandingCopyExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingCopyExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, STAGE_CODE, includeCompleted);
    return ResponseEntity.ok(response);
  }

  /** Retorna os detalhes de uma execução específica da etapa. */
  @GetMapping("/copy/stage-executions/{idJob}")
  public ResponseEntity<GeraLandingCopyStageExecutionDetailResponse> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    GeraLandingCopyStageExecutionDetailResponse response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
