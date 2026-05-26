package com.marketinghub.geralanding.wireframe.web;

import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import com.marketinghub.geralanding.GeraLandingStartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-wireframe no GeraLanding. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingWireframeController {
  private final GeraLandingStageExecutionService executionService;

  public GeraLandingWireframeController(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-wireframe. */
  @PostMapping("/wireframe/start")
  public ResponseEntity<GeraLandingStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingStartResponse response = executionService.registerInitialExecution(experimentId, "landing-page-wireframe");
    return ResponseEntity.accepted().body(response);
  }
}
