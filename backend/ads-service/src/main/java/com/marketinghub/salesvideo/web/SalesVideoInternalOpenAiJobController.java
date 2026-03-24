package com.marketinghub.salesvideo.web;

import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.dto.*;
import com.marketinghub.salesvideo.service.SalesVideoJobService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contratos REST consumidos pelo ai-worker para jobs OpenAI.
 */
@RestController
@RequestMapping("/internal/ai/openai-jobs")
public class SalesVideoInternalOpenAiJobController {
    private final SalesVideoJobService jobService;

    public SalesVideoInternalOpenAiJobController(SalesVideoJobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public List<SalesVideoJobDto> listJobs(@RequestParam(required = false) SalesVideoStatus status,
                                           @RequestParam(required = false, name = "type") SalesVideoJobType jobType,
                                           @RequestParam(defaultValue = "25") int limit) {
        return jobService.findJobs(SalesVideoProviderFamily.OPENAI, status, jobType, limit);
    }

    @GetMapping("/{jobId}")
    public SalesVideoJobDto getJob(@PathVariable Long jobId) {
        return jobService.getJob(jobId);
    }

    @PostMapping("/{jobId}/claim")
    public SalesVideoJobDto claim(@PathVariable Long jobId,
                                  @Valid @RequestBody JobClaimRequest request) {
        return jobService.claimJob(jobId, request);
    }

    @PostMapping("/{jobId}/heartbeat")
    public SalesVideoJobDto heartbeat(@PathVariable Long jobId,
                                      @RequestBody JobHeartbeatRequest request) {
        return jobService.heartbeat(jobId, request);
    }

    @PostMapping("/{jobId}/progress")
    public SalesVideoJobDto progress(@PathVariable Long jobId,
                                     @RequestBody JobProgressRequest request) {
        return jobService.progress(jobId, request);
    }

    @PostMapping("/{jobId}/complete")
    public SalesVideoJobDto complete(@PathVariable Long jobId,
                                     @RequestBody JobCompletionRequest request) {
        return jobService.complete(jobId, request);
    }

    @PostMapping("/{jobId}/fail")
    public SalesVideoJobDto fail(@PathVariable Long jobId,
                                 @RequestBody JobFailureRequest request) {
        return jobService.fail(jobId, request);
    }
}
