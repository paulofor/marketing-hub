package com.marketinghub.geralanding.execution.web;

import com.marketinghub.geralanding.GeraLandingExecutionSummaryResponse;
import com.marketinghub.geralanding.GeraLandingProvisionalHtmlResponse;
import com.marketinghub.geralanding.GeraLandingPublishResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionDetailResponse;
import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsável pelos endpoints transversais de execução do GeraLanding. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingExecutionController {
  private final GeraLandingStageExecutionService executionService;

  public GeraLandingExecutionController(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Lista execuções de uma etapa do experimento. */
  @GetMapping("/stage-executions")
  public ResponseEntity<List<GeraLandingExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "landing-page-wireframe") String stageCode,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    return ResponseEntity.ok(
        executionService.listExperimentStageExecutions(experimentId, stageCode, includeCompleted));
  }

  /** Retorna o detalhamento de uma execução específica. */
  @GetMapping("/stage-executions/{idJob}")
  public ResponseEntity<GeraLandingStageExecutionDetailResponse> getStageExecutionDetail(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    return ResponseEntity.ok(executionService.getStageExecutionDetail(experimentId, idJob));
  }

  /** Gera e persiste HTML provisório da execução. */
  @PostMapping("/html/provisional/generate")
  public ResponseEntity<GeraLandingProvisionalHtmlResponse> generateProvisionalHtml(
      @PathVariable Long experimentId, @RequestParam String jobId) {
    String provisionalHtml =
        executionService.generateAndPersistProvisionalHtmlFromExperiment(experimentId, jobId);
    return ResponseEntity.ok(new GeraLandingProvisionalHtmlResponse(provisionalHtml));
  }

  /** Aprova e publica a landing final do experimento. */
  @PostMapping("/landing/approve-and-publish")
  public ResponseEntity<GeraLandingPublishResponse> approveAndPublishLanding(
      @PathVariable Long experimentId) {
    return ResponseEntity.ok(executionService.approveAndPublishLanding(experimentId));
  }
}
