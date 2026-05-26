package com.marketinghub.geralanding.wireframe.web;

import com.marketinghub.geralanding.GeraLandingExecutionSummaryResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionDetailResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeStageService;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeStartResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-wireframe no GeraLanding e expor consultas das execuções da etapa. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingWireframeController {
  private static final String STAGE_CODE = "landing-page-wireframe";

  private final GeraLandingWireframeStageService stageService;
  private final GeraLandingStageExecutionService executionService;

  public GeraLandingWireframeController(GeraLandingWireframeStageService stageService, GeraLandingStageExecutionService executionService) {
    this.stageService = stageService;
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-wireframe. */
  @PostMapping("/wireframe/start")
  public ResponseEntity<GeraLandingWireframeStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingWireframeStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }

  /** Lista as execuções da etapa para o experimento. */
  @GetMapping("/wireframe/stage-executions")
  public ResponseEntity<List<GeraLandingExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, STAGE_CODE, includeCompleted);
    return ResponseEntity.ok(response);
  }

  /** Retorna os detalhes de uma execução específica da etapa. */
  @GetMapping("/wireframe/stage-executions/{idJob}")
  public ResponseEntity<GeraLandingStageExecutionDetailResponse> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    GeraLandingStageExecutionDetailResponse response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
