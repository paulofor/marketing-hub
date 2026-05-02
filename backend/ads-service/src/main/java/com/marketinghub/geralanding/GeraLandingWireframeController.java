package com.marketinghub.geralanding;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiments/{experimentId}/geralanding")
public class GeraLandingWireframeController {

  @PostMapping("/wireframe/start")
  public ResponseEntity<Void> startWireframe(@PathVariable Long experimentId) {
    return ResponseEntity.accepted().build();
  }
}
