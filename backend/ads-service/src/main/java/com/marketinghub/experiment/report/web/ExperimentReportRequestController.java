package com.marketinghub.experiment.report.web;

import com.marketinghub.experiment.report.dto.CreateExperimentReportRequest;
import com.marketinghub.experiment.report.dto.ExperimentReportRequestDto;
import com.marketinghub.experiment.report.mapper.ExperimentReportRequestMapper;
import com.marketinghub.experiment.report.service.ExperimentReportRequestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Operações vinculadas a um experimento específico.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/report-requests")
public class ExperimentReportRequestController {

    private final ExperimentReportRequestService service;
    private final ExperimentReportRequestMapper mapper;

    public ExperimentReportRequestController(ExperimentReportRequestService service,
                                             ExperimentReportRequestMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ExperimentReportRequestDto> list(@PathVariable Long experimentId) {
        return service.listLatestByExperiment(experimentId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @PostMapping
    public ExperimentReportRequestDto create(@PathVariable Long experimentId,
                                             @Valid @RequestBody(required = false) CreateExperimentReportRequest request) {
        String requestedBy = request != null ? request.requestedBy() : null;
        return mapper.toDto(service.create(experimentId, requestedBy));
    }
}
