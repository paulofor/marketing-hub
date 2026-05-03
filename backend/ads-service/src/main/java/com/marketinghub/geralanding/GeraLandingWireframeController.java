package com.marketinghub.geralanding;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingWireframeController {

  private final GeraLandingStageExecutionService executionService;

  public GeraLandingWireframeController(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }

  @PostMapping("/wireframe/start")
  public ResponseEntity<GeraLandingStartResponse> startWireframe(@PathVariable Long experimentId) {
    GeraLandingStartResponse response = executionService.registerInitialExecution(experimentId);
    return ResponseEntity.accepted().body(response);
  }
}
