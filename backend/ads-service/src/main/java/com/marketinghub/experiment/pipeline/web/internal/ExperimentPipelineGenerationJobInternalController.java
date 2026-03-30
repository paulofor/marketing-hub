package com.marketinghub.experiment.pipeline.web.internal;

import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobClaimRequest;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobCompletionRequest;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobDto;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobFailureRequest;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobStageUpdateRequest;
import com.marketinghub.experiment.pipeline.service.ExperimentPipelineGenerationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/experiment-pipeline/jobs")
public class ExperimentPipelineGenerationJobInternalController {
    private final ExperimentPipelineGenerationService service;

    public ExperimentPipelineGenerationJobInternalController(ExperimentPipelineGenerationService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public List<ExperimentPipelineGenerationJobDto> listPending(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return service.listPendingJobs(limit != null ? limit : 10);
    }

    @PostMapping("/{jobId}/claim")
    public ExperimentPipelineGenerationJobDto claim(@PathVariable UUID jobId,
                                                    @Valid @RequestBody ExperimentPipelineGenerationJobClaimRequest request) {
        return service.claimJob(jobId, request.workerId());
    }

    @PostMapping("/{jobId}/complete")
    public void complete(@PathVariable UUID jobId,
                         @Valid @RequestBody ExperimentPipelineGenerationJobCompletionRequest request) {
        service.completeJob(jobId, request);
    }

    @PostMapping("/{jobId}/fail")
    public void fail(@PathVariable UUID jobId,
                     @Valid @RequestBody ExperimentPipelineGenerationJobFailureRequest request) {
        service.failJob(jobId, request.errorMessage());
    }

    @PostMapping("/{jobId}/stage")
    public void updateStage(@PathVariable UUID jobId,
                            @Valid @RequestBody ExperimentPipelineGenerationJobStageUpdateRequest request) {
        service.updateJobStage(jobId, request.stage());
    }
}
