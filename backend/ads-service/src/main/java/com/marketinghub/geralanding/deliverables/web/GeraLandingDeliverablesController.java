package com.marketinghub.geralanding.deliverables.web;

import com.marketinghub.geralanding.deliverables.GeraLandingDeliverablesStageService;
import com.marketinghub.geralanding.deliverables.GeraLandingDeliverablesStartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-deliverables no GeraLanding. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingDeliverablesController {
  private final GeraLandingDeliverablesStageService stageService;

  public GeraLandingDeliverablesController(GeraLandingDeliverablesStageService stageService) {
    this.stageService = stageService;
  }

  /** Registra uma execução inicial da etapa landing-page-deliverables. */
  @PostMapping("/deliverables/start")
  public ResponseEntity<GeraLandingDeliverablesStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingDeliverablesStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }
}
