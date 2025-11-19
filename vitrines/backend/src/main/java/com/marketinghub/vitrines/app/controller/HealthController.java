package com.marketinghub.vitrines.app.controller;

import com.marketinghub.vitrines.app.model.HealthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

  private final BuildProperties buildProperties;

  public HealthController(@Autowired(required = false) BuildProperties buildProperties) {
    this.buildProperties = buildProperties;
  }

  @GetMapping("/health")
  public HealthResponse health() {
    String version = buildProperties != null ? buildProperties.getVersion() : "development";
    return new HealthResponse("UP", version);
  }
}
