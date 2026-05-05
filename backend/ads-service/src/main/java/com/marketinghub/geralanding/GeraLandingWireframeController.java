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
public class GeraLandingWireframeController {

  private final GeraLandingStageExecutionService executionService;

  public GeraLandingWireframeController(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }


  @GetMapping("/stage-executions")
  public ResponseEntity<List<GeraLandingExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "landing-page-wireframe") String stageCode) {
    List<GeraLandingExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, stageCode);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/wireframe/start")
  public ResponseEntity<GeraLandingStartResponse> startWireframe(@PathVariable Long experimentId) {
    GeraLandingStartResponse response = executionService.registerInitialExecution(experimentId);
    return ResponseEntity.accepted().body(response);
  }
}
