package com.marketinghub.geralanding.imageplanning.web;

import com.marketinghub.geralanding.imageplanning.service.GeraLandingImagePlanningStageService;
import com.marketinghub.geralanding.imageplanning.service.GeraLandingImagePlanningStartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-image-planning no GeraLanding. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingImagePlanningController {
  private final GeraLandingImagePlanningStageService stageService;

  public GeraLandingImagePlanningController(GeraLandingImagePlanningStageService stageService) {
    this.stageService = stageService;
  }

  /** Registra uma execução inicial da etapa landing-page-image-planning. */
  @PostMapping("/image-prompts/start")
  public ResponseEntity<GeraLandingImagePlanningStartResponse> start(@PathVariable Long experimentId) {    GeraLandingImagePlanningStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }
}
