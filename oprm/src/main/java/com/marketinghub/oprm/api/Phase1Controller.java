package com.marketinghub.oprm.api;

import com.marketinghub.oprm.application.OccupationResolverService;
import com.marketinghub.oprm.domain.ArtifactEnvelope;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oprm/phase1")
public class Phase1Controller {

    private final OccupationResolverService occupationResolverService;

    public Phase1Controller(OccupationResolverService occupationResolverService) {
        this.occupationResolverService = occupationResolverService;
    }

    @GetMapping("/supported-occupations")
    public ResponseEntity<List<String>> supportedOccupations() {
        return ResponseEntity.ok(occupationResolverService.supportedOccupations());
    }

    @PostMapping("/resolve")
    public ResponseEntity<ArtifactEnvelope> resolve(@Valid @RequestBody Phase1ResolveRequest request) {
        ArtifactEnvelope result = occupationResolverService.resolveToProfileSnapshot(
                request.occupationLabel(),
                request.nicheName(),
                request.locale(),
                request.correlationId()
        );
        return ResponseEntity.ok(result);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
