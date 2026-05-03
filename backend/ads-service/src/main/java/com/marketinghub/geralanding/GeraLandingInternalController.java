package com.marketinghub.geralanding;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/geralanding")
public class GeraLandingInternalController {

    private final GeraLandingStageExecutionService executionService;

    public GeraLandingInternalController(GeraLandingStageExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/stage-executions")
    public ResponseEntity<Void> registerWorkerPrompt(@Valid @RequestBody GeraLandingWorkerPromptRequest request) {
        executionService.registerWorkerPromptExecution(request);
        return ResponseEntity.accepted().build();
    }
}
