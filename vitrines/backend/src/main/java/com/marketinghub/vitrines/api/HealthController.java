package com.marketinghub.vitrines.api;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

  @GetMapping
  public ResponseEntity<Map<String, String>> getHealth() {
    Map<String, String> payload = new HashMap<>();
    payload.put("status", "ok");
    payload.put("version", OffsetDateTime.now().toString());
    return ResponseEntity.ok(payload);
  }
}
