package com.marketinghub.experiment.report.web;

import com.marketinghub.experiment.report.dto.ExperimentCompleteMarkdownReportDto;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto;
import com.marketinghub.experiment.report.service.ExperimentCompleteMarkdownReportService;
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
    private final ExperimentCompleteMarkdownReportService completeMarkdownReportService;

    /** Inicializa o controller com serviços de material estruturado e relatório Markdown. */
    public ExperimentReportMaterialController(ExperimentReportMaterialService service,
                                              ExperimentCompleteMarkdownReportService completeMarkdownReportService) {
        this.service = service;
        this.completeMarkdownReportService = completeMarkdownReportService;
    }

    /** Retorna o pacote estruturado de dados usado por prévias e workers de relatório. */
    @GetMapping
    public ExperimentReportMaterialDto getMaterial(@PathVariable Long experimentId) {
        return service.build(experimentId);
    }

    /** Retorna o relatório completo do experimento em Markdown para download pelo usuário. */
    @GetMapping("/complete-markdown")
    public ExperimentCompleteMarkdownReportDto getCompleteMarkdown(@PathVariable Long experimentId) {
        return new ExperimentCompleteMarkdownReportDto(
                completeMarkdownReportService.buildFilename(experimentId),
                completeMarkdownReportService.buildMarkdown(experimentId)
        );
    }
}
