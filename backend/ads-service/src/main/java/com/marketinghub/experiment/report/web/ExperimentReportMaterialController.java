package com.marketinghub.experiment.report.web;

import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto;
import com.marketinghub.experiment.report.service.ExperimentReportMaterialService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint que expõe o pacote de dados usado para montar o relatório.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/report-material")
public class ExperimentReportMaterialController {

    private final ExperimentReportMaterialService service;

    public ExperimentReportMaterialController(ExperimentReportMaterialService service) {
        this.service = service;
    }

    @GetMapping
    public ExperimentReportMaterialDto getMaterial(@PathVariable Long experimentId) {
        return service.build(experimentId);
    }
}
