package com.marketinghub.geralanding.deliverables.web;

import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import com.marketinghub.geralanding.GeraLandingStartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-deliverables no GeraLanding. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingDeliverablesController {
  private final GeraLandingStageExecutionService executionService;

  public GeraLandingDeliverablesController(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-deliverables. */
  @PostMapping("/deliverables/start")
  public ResponseEntity<GeraLandingStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingStartResponse response = executionService.registerInitialExecution(experimentId, "landing-page-deliverables");
    return ResponseEntity.accepted().body(response);
  }
}
