package com.marketinghub.experiment.frameworkimage.web;

import com.marketinghub.experiment.frameworkimage.dto.FrameworkImageGenerationItemStatusDto;
import com.marketinghub.experiment.frameworkimage.dto.FrameworkImageGenerationSummaryDto;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobDto;
import com.marketinghub.experiment.frameworkimage.service.FrameworkImageGenerationService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiments/{experimentId}/framework-images")
public class FrameworkImageGenerationController {
    private final FrameworkImageGenerationService service;

    public FrameworkImageGenerationController(FrameworkImageGenerationService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<FrameworkImageGenerationJobDto> generate(@PathVariable Long experimentId) {
        return service.enqueueJobsForExperiment(experimentId);
    }

    @GetMapping
    public List<FrameworkImageGenerationItemStatusDto> list(@PathVariable Long experimentId) {
        return service.listJobsByExperiment(experimentId);
    }

    @GetMapping("/summary")
    public FrameworkImageGenerationSummaryDto summary(@PathVariable Long experimentId) {
        return service.summarizeJobsByExperiment(experimentId);
    }
}
