package com.marketinghub.experiment.pipeline.web;

import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationRequest;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobDto;
import com.marketinghub.experiment.pipeline.service.ExperimentPipelineGenerationService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/experiments/{id}/pipeline")
public class ExperimentPipelineController {
    private final ExperimentPipelineGenerationService generationService;

    public ExperimentPipelineController(ExperimentPipelineGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/{section}/generate")
    public ExperimentDto generate(@PathVariable Long id,
                                  @PathVariable String section,
                                  @RequestBody(required = false) ExperimentPipelineGenerationRequest request) {
        ExperimentPipelineSection parsed;
        try {
            parsed = ExperimentPipelineSection.fromPath(section);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        ExperimentPipelineGenerationRequest payload = request != null ? request : new ExperimentPipelineGenerationRequest();
        return generationService.generate(id, parsed, payload);
    }

    @GetMapping("/jobs")
    public List<ExperimentPipelineGenerationJobDto> listJobs(@PathVariable Long id,
                                                             @RequestParam(value = "size", defaultValue = "30") Integer size) {
        return generationService.listJobs(id, size != null ? size : 30);
    }
}
