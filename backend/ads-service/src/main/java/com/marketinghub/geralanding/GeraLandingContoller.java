package com.marketinghub.geralanding;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingContoller {

  private final GeraLandingStageExecutionService executionService;

  public GeraLandingContoller(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }


  @GetMapping("/stage-executions")
  public ResponseEntity<List<GeraLandingExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "landing-page-wireframe") String stageCode,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, stageCode, includeCompleted);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/stage-executions/{idJob}")
  public ResponseEntity<GeraLandingStageExecutionDetailResponse> getStageExecutionDetail(
      @PathVariable Long experimentId,
      @PathVariable String idJob) {
    return ResponseEntity.ok(executionService.getStageExecutionDetail(experimentId, idJob));
  }

  @PostMapping("/wireframe/start")
  public ResponseEntity<GeraLandingStartResponse> startWireframe(@PathVariable Long experimentId) {
    GeraLandingStartResponse response = executionService.registerInitialExecution(experimentId, "landing-page-wireframe");
    return ResponseEntity.accepted().body(response);
  }

  @PostMapping("/copy/start")
  public ResponseEntity<GeraLandingStartResponse> startCopy(@PathVariable Long experimentId) {
    GeraLandingStartResponse response = executionService.registerInitialExecution(experimentId, "landing-page-copy");
    return ResponseEntity.accepted().body(response);
  }

  @PostMapping("/image-prompts/start")
  public ResponseEntity<GeraLandingStartResponse> startImagePrompts(@PathVariable Long experimentId) {
    GeraLandingStartResponse response = executionService.registerInitialExecution(experimentId, "landing-page-image-planning");
    return ResponseEntity.accepted().body(response);
  }

  @PostMapping("/html/provisional/generate")
  public ResponseEntity<GeraLandingProvisionalHtmlResponse> generateProvisionalHtml(
      @PathVariable Long experimentId,
      @RequestParam String jobId) {
    String provisionalHtml = executionService.generateAndPersistProvisionalHtmlFromExperiment(experimentId, jobId);
    return ResponseEntity.ok(new GeraLandingProvisionalHtmlResponse(provisionalHtml));
  }
}
