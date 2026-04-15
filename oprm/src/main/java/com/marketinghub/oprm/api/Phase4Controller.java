package com.marketinghub.oprm.api;

import com.marketinghub.oprm.application.FrameworkIntegrationService;
import com.marketinghub.oprm.domain.ArtifactEnvelope;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/oprm/phase4")
public class Phase4Controller {

    private final FrameworkIntegrationService frameworkIntegrationService;

    public Phase4Controller(FrameworkIntegrationService frameworkIntegrationService) {
        this.frameworkIntegrationService = frameworkIntegrationService;
    }

    @PostMapping("/integrate")
    public ResponseEntity<ArtifactEnvelope> integrate(@Valid @RequestBody Phase4IntegrateRequest request) {
        ArtifactEnvelope result = frameworkIntegrationService.integrateRoutineSignals(
                request.occupationLabel(),
                request.nicheName(),
                request.locale(),
                request.correlationId()
        );
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
