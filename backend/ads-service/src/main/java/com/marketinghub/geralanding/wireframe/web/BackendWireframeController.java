package com.marketinghub.geralanding.wireframe.web;

import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeExecutionSummaryResponse;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframePendingExecutionResponse;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeStageExecutionDetailResponse;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeStageExecutionService;
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

/** Responsável por expor os endpoints de backend da etapa landing-page-wireframe no GeraLanding. */
@RestController
@RequestMapping("/api")
public class BackendWireframeController {
  private static final String STAGE_CODE = "landing-page-wireframe";

  private final GeraLandingWireframeStageService stageService;
  private final GeraLandingWireframeStageExecutionService executionService;

  /** Inicializa o controller com os serviços da etapa wireframe. */
  public BackendWireframeController(
      GeraLandingWireframeStageService stageService,
      GeraLandingWireframeStageExecutionService executionService) {
    this.stageService = stageService;
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-wireframe. */
  @PostMapping("/experiments/{experimentId}/geralanding/wireframe/start")
  public ResponseEntity<GeraLandingWireframeStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingWireframeStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }

  /** Lista as execuções da etapa para o experimento. */
  @GetMapping("/experiments/{experimentId}/geralanding/wireframe/stage-executions")
  public ResponseEntity<List<GeraLandingWireframeExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingWireframeExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, STAGE_CODE, includeCompleted);
    return ResponseEntity.ok(response);
  }

  /** Lista os jobs pendentes iniciados da etapa wireframe para processamento pelo Worker AI. */
  @GetMapping("/internal/geralanding/wireframe/stage-executions/pending")
  public ResponseEntity<List<GeraLandingWireframePendingExecutionResponse>> pending() {
    List<GeraLandingWireframePendingExecutionResponse> response =
        executionService.listPending(STAGE_CODE);
    return ResponseEntity.ok(response);
  }

  /** Retorna os detalhes de uma execução específica da etapa. */
  @GetMapping("/experiments/{experimentId}/geralanding/wireframe/stage-executions/{idJob}")
  public ResponseEntity<GeraLandingWireframeStageExecutionDetailResponse> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    GeraLandingWireframeStageExecutionDetailResponse response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
