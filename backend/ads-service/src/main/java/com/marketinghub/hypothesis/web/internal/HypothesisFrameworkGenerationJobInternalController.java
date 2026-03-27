package com.marketinghub.hypothesis.web.internal;

import com.marketinghub.hypothesis.dto.internal.HypothesisFrameworkGenerationJobClaimRequest;
import com.marketinghub.hypothesis.dto.internal.HypothesisFrameworkGenerationJobCompletionRequest;
import com.marketinghub.hypothesis.dto.internal.HypothesisFrameworkGenerationJobDto;
import com.marketinghub.hypothesis.dto.internal.HypothesisFrameworkGenerationJobFailureRequest;
import com.marketinghub.hypothesis.service.HypothesisFrameworkGenerationService;
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
@RequestMapping("/api/internal/hypothesis-framework/jobs")
public class HypothesisFrameworkGenerationJobInternalController {
    private final HypothesisFrameworkGenerationService service;

    public HypothesisFrameworkGenerationJobInternalController(HypothesisFrameworkGenerationService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public List<HypothesisFrameworkGenerationJobDto> listPending(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return service.listPendingJobs(limit != null ? limit : 10);
    }

    @PostMapping("/{jobId}/claim")
    public HypothesisFrameworkGenerationJobDto claim(@PathVariable UUID jobId,
                                                     @Valid @RequestBody HypothesisFrameworkGenerationJobClaimRequest request) {
        return service.claimJob(jobId, request.workerId());
    }

    @PostMapping("/{jobId}/complete")
    public void complete(@PathVariable UUID jobId,
                         @Valid @RequestBody HypothesisFrameworkGenerationJobCompletionRequest request) {
        service.completeJob(jobId, request);
    }

    @PostMapping("/{jobId}/fail")
    public void fail(@PathVariable UUID jobId,
                     @Valid @RequestBody HypothesisFrameworkGenerationJobFailureRequest request) {
        service.failJob(jobId, request.errorMessage());
    }
}
