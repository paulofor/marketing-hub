package com.marketinghub.geralanding.copy.web;

import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import com.marketinghub.geralanding.GeraLandingStartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-copy no GeraLanding. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingCopyController {
  private final GeraLandingStageExecutionService executionService;

  public GeraLandingCopyController(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-copy. */
  @PostMapping("/copy/start")
  public ResponseEntity<GeraLandingStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingStartResponse response = executionService.registerInitialExecution(experimentId, "landing-page-copy");
    return ResponseEntity.accepted().body(response);
  }
}
