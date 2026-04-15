package com.marketinghub.oprm.api;

import com.marketinghub.oprm.application.RoutineInferenceService;
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
@RequestMapping("/api/oprm/phase3")
public class Phase3Controller {

    private final RoutineInferenceService routineInferenceService;

    public Phase3Controller(RoutineInferenceService routineInferenceService) {
        this.routineInferenceService = routineInferenceService;
    }

    @PostMapping("/infer")
    public ResponseEntity<ArtifactEnvelope> infer(@Valid @RequestBody Phase3InferRequest request) {
        ArtifactEnvelope result = routineInferenceService.inferRoutine(
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
