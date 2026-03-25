package com.marketinghub.salesvideo.web;

import com.marketinghub.salesvideo.dto.RetrySalesVideoJobRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.dto.SalesVideoJobEventDto;
import com.marketinghub.salesvideo.service.SalesVideoJobService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints administrativos para acompanhar e reprocessar jobs.
 */
@RestController
@RequestMapping("/api/sales-videos")
public class SalesVideoJobAdminController {
    private final SalesVideoJobService jobService;

    public SalesVideoJobAdminController(SalesVideoJobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/profiles/{profileId}/jobs")
    public List<SalesVideoJobDto> listJobsByProfile(@PathVariable Long profileId) {
        return jobService.listJobsByProfile(profileId);
    }

    @GetMapping("/jobs/{jobId}")
    public SalesVideoJobDto getJob(@PathVariable Long jobId) {
        return jobService.getJob(jobId);
    }

    @GetMapping("/jobs/{jobId}/events")
    public List<SalesVideoJobEventDto> getJobEvents(@PathVariable Long jobId) {
        return jobService.getJobEvents(jobId);
    }

    @PostMapping("/jobs/{jobId}/retry")
    public SalesVideoJobDto retryJob(@PathVariable Long jobId,
                                     @Valid @RequestBody RetrySalesVideoJobRequest request) {
        return jobService.retry(jobId, request);
    }
}
