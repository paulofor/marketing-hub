package com.marketinghub.experiment.pipeline.web;

import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationJobDetailDto;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationJobSummaryDto;
import com.marketinghub.experiment.pipeline.dto.ExperimentPipelineGenerationRequest;
import com.marketinghub.experiment.pipeline.dto.internal.ExperimentPipelineGenerationJobDto;
import com.marketinghub.experiment.pipeline.service.ExperimentPipelineGenerationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
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

    @PostMapping("/landing-page-html/apply-to-form")
    public ExperimentDto applyLandingHtmlToForm(@PathVariable Long id) {
        return generationService.applyLandingHtmlToLeadPortalForm(id);
    }

    @GetMapping("/jobs")
    public List<ExperimentPipelineGenerationJobDto> listJobs(@PathVariable Long id,
                                                             @RequestParam(value = "size", defaultValue = "30") Integer size) {
        return generationService.listJobs(id, size != null ? size : 30);
    }

    @GetMapping("/jobs/history")
    public Page<ExperimentPipelineGenerationJobSummaryDto> listJobHistory(
            @PathVariable Long id,
            @RequestParam(value = "section", required = false) String section,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        ExperimentPipelineSection parsedSection = parseSection(section);
        return generationService.listJobsPage(
                id,
                parsedSection,
                page != null ? page : 0,
                size != null ? size : 20);
    }

    @GetMapping("/jobs/total-cost-usd")
    public BigDecimal getTotalCostUsd(@PathVariable Long id) {
        return generationService.totalCostUsd(id);
    }

    @GetMapping("/jobs/{jobId}")
    public ExperimentPipelineGenerationJobDetailDto getJobDetail(@PathVariable Long id,
                                                                  @PathVariable UUID jobId) {
        return generationService.getJobDetail(id, jobId);
    }

    private ExperimentPipelineSection parseSection(String section) {
        if (section == null || section.isBlank()) {
            return null;
        }
        try {
            return ExperimentPipelineSection.fromPath(section.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
