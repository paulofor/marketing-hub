package com.marketinghub.experiment.frameworkimage.web.internal;

import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobClaimRequest;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobCompletionRequest;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobDto;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobFailureRequest;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobStageUpdateRequest;
import com.marketinghub.experiment.frameworkimage.service.FrameworkImageGenerationService;
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
@RequestMapping("/api/internal/framework-image/jobs")
public class FrameworkImageGenerationJobInternalController {
    private final FrameworkImageGenerationService service;

    public FrameworkImageGenerationJobInternalController(FrameworkImageGenerationService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public List<FrameworkImageGenerationJobDto> listPending(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return service.listPendingJobs(limit != null ? limit : 10);
    }

    @PostMapping("/{jobId}/claim")
    public FrameworkImageGenerationJobDto claim(@PathVariable UUID jobId,
                                                @Valid @RequestBody FrameworkImageGenerationJobClaimRequest request) {
        return service.claimJob(jobId, request.workerId());
    }

    @PostMapping("/{jobId}/stage")
    public void updateStage(@PathVariable UUID jobId,
                            @Valid @RequestBody FrameworkImageGenerationJobStageUpdateRequest request) {
        service.updateJobStage(jobId, request.stage());
    }

    @PostMapping("/{jobId}/complete")
    public void complete(@PathVariable UUID jobId,
                         @RequestBody FrameworkImageGenerationJobCompletionRequest request) {
        service.completeJob(jobId, request);
    }

    @PostMapping("/{jobId}/fail")
    public void fail(@PathVariable UUID jobId,
                     @Valid @RequestBody FrameworkImageGenerationJobFailureRequest request) {
        service.failJob(jobId, request.errorMessage());
    }
}
