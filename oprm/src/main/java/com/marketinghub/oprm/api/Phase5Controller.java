package com.marketinghub.oprm.api;

import com.marketinghub.oprm.application.FeedbackLoopService;
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
@RequestMapping("/api/oprm/phase5")
public class Phase5Controller {

    private final FeedbackLoopService feedbackLoopService;

    public Phase5Controller(FeedbackLoopService feedbackLoopService) {
        this.feedbackLoopService = feedbackLoopService;
    }

    @PostMapping("/feedback")
    public ResponseEntity<ArtifactEnvelope> feedback(@Valid @RequestBody Phase5FeedbackRequest request) {
        ArtifactEnvelope result = feedbackLoopService.recalibrateWithFeedback(
                request.occupationLabel(),
                request.nicheName(),
                request.locale(),
                request.correlationId(),
                request.hypothesisPerformance()
        );
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
