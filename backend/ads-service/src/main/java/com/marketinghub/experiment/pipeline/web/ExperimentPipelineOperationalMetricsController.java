package com.marketinghub.experiment.pipeline.web;

import com.marketinghub.experiment.pipeline.dto.PipelineOperationalMetricsDto;
import com.marketinghub.experiment.pipeline.service.PipelineOperationalMetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiments/pipeline")
public class ExperimentPipelineOperationalMetricsController {
    private final PipelineOperationalMetricsService metricsService;

    public ExperimentPipelineOperationalMetricsController(PipelineOperationalMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/operational-metrics")
    public PipelineOperationalMetricsDto operationalMetrics(
            @RequestParam(name = "limit", defaultValue = "300") int limit) {
        return metricsService.collect(limit);
    }
}

