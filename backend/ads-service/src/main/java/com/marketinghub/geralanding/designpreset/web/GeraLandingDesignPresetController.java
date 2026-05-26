package com.marketinghub.geralanding.designpreset.web;

import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetStageService;
import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetStartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por iniciar a etapa landing-page-design-preset no GeraLanding. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingDesignPresetController {
  private final GeraLandingDesignPresetStageService stageService;

  public GeraLandingDesignPresetController(GeraLandingDesignPresetStageService stageService) {
    this.stageService = stageService;
  }

  /** Registra uma execução inicial da etapa landing-page-design-preset. */
  @PostMapping("/design-preset/start")
  public ResponseEntity<GeraLandingDesignPresetStartResponse> start(@PathVariable Long experimentId) {    GeraLandingDesignPresetStartResponse response = stageService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }
}
