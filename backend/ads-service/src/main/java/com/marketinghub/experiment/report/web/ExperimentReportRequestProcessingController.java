package com.marketinghub.experiment.report.web;

import com.marketinghub.experiment.report.ExperimentReportStatus;
import com.marketinghub.experiment.report.dto.ExperimentReportRequestDetailDto;
import com.marketinghub.experiment.report.dto.UpdateExperimentReportRequest;
import com.marketinghub.experiment.report.mapper.ExperimentReportRequestMapper;
import com.marketinghub.experiment.report.service.ExperimentReportRequestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints usados pelos demais serviços para monitorar e atualizar solicitações.
 */
@RestController
@RequestMapping("/api/experiment-report-requests")
public class ExperimentReportRequestProcessingController {

    private final ExperimentReportRequestService service;
    private final ExperimentReportRequestMapper mapper;

    public ExperimentReportRequestProcessingController(ExperimentReportRequestService service,
                                                       ExperimentReportRequestMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ExperimentReportRequestDetailDto> list(@RequestParam(name = "status", required = false) List<ExperimentReportStatus> statuses) {
        return service.listByStatus(statuses).stream()
                .map(mapper::toDetailDto)
                .toList();
    }

    @GetMapping("/{id}")
    public ExperimentReportRequestDetailDto get(@PathVariable Long id) {
        return mapper.toDetailDto(service.get(id));
    }

    @PatchMapping("/{id}")
    public ExperimentReportRequestDetailDto update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateExperimentReportRequest request) {
        return mapper.toDetailDto(service.updateStatus(id, request));
    }
}
