package com.marketinghub.geralanding.designpreset.web;

import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import com.marketinghub.geralanding.GeraLandingStartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-design-preset no GeraLanding. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingDesignPresetController {
  private final GeraLandingStageExecutionService executionService;

  public GeraLandingDesignPresetController(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-design-preset. */
  @PostMapping("/design-preset/start")
  public ResponseEntity<GeraLandingStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingStartResponse response = executionService.registerInitialExecution(experimentId, "landing-page-design-preset");
    return ResponseEntity.accepted().body(response);
  }
}
