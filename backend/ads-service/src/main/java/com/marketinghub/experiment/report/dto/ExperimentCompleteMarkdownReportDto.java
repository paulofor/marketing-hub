package com.marketinghub.experiment.report.dto;

/**
 * Resposta com o relatório completo do experimento em Markdown.
 */
public record ExperimentCompleteMarkdownReportDto(String filename, String markdown) {
}
