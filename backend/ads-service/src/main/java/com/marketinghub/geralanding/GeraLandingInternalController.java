package com.marketinghub.geralanding;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PostMapping("/stage-executions/{idJob}/receive-prompt")
    public ResponseEntity<Void> receivePrompt(@PathVariable String idJob,
                                              @Valid @RequestBody GeraLandingPromptReceiveRequest request) {
        executionService.receivePrompt(idJob, request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/stage-executions/receive-prompt")
    public ResponseEntity<Void> receivePromptDirect(@Valid @RequestBody GeraLandingPromptReceiveDirectRequest request) {
        executionService.receivePrompt(request.idJob(),
                new GeraLandingPromptReceiveRequest(request.experimentId(), request.stageCode(), request.prompt(), null, null, null, null));
        return ResponseEntity.accepted().build();
    }



    @PostMapping("/stage-executions/{idJob}/receive-result")
    public ResponseEntity<Void> receiveResult(@PathVariable String idJob,
                                              @Valid @RequestBody GeraLandingResultReceiveRequest request) {
        executionService.receiveResult(idJob, request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/stage-executions/pending")
    public ResponseEntity<List<GeraLandingPendingExecutionResponse>> listPendingExecutions() {
        return ResponseEntity.ok(executionService.listPendingExecutions());
    }
}
