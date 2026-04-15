package com.marketinghub.oprm.api;

import com.marketinghub.oprm.application.WebEnrichmentService;
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
@RequestMapping("/api/oprm/phase2")
public class Phase2Controller {

    private final WebEnrichmentService webEnrichmentService;

    public Phase2Controller(WebEnrichmentService webEnrichmentService) {
        this.webEnrichmentService = webEnrichmentService;
    }

    @PostMapping("/enrich")
    public ResponseEntity<ArtifactEnvelope> enrich(@Valid @RequestBody Phase2EnrichRequest request) {
        ArtifactEnvelope result = webEnrichmentService.enrichOccupation(
                request.occupationLabel(),
                request.nicheName(),
                request.locale(),
                request.correlationId());
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
