package com.marketinghub.geralanding.copy.web;

import com.marketinghub.geralanding.copy.service.GeraLandingCopyStageService;
import com.marketinghub.geralanding.copy.service.GeraLandingCopyStartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-copy no GeraLanding. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingCopyController {
  private final GeraLandingCopyStageService stageService;

  public GeraLandingCopyController(GeraLandingCopyStageService stageService) {
    this.stageService = stageService;
  }

  /** Registra uma execução inicial da etapa landing-page-copy. */
  @PostMapping("/copy/start")
  public ResponseEntity<GeraLandingCopyStartResponse> start(@PathVariable Long experimentId) {    GeraLandingCopyStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }
}
