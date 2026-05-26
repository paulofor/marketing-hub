package com.marketinghub.geralanding.wireframe.web;

import com.marketinghub.geralanding.wireframe.GeraLandingWireframeStageService;
import com.marketinghub.geralanding.wireframe.GeraLandingWireframeStartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-wireframe no GeraLanding. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingWireframeController {
  private final GeraLandingWireframeStageService stageService;

  public GeraLandingWireframeController(GeraLandingWireframeStageService stageService) {
    this.stageService = stageService;
  }

  /** Registra uma execução inicial da etapa landing-page-wireframe. */
  @PostMapping("/wireframe/start")
  public ResponseEntity<GeraLandingWireframeStartResponse> start(@PathVariable Long experimentId) {    GeraLandingWireframeStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }
}
