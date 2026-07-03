package com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.pending;

import com.marketinghub.moissaleslibraryworker.pipelines.shared.service.PipelinePromptSchemaTemplatePayload;
import java.util.Map;

/** Contrato de trabalho pendente entregue ao worker para extrair padrões de página. */
public record SalesPagePatternsPagePatternExtractionPendingJob(
        String jobId,
        long stageExecutionId,
        long dossierId,
        String workspaceId,
        String stageName,
        Map<String, Object> input,
        PipelinePromptSchemaTemplatePayload promptSchemaTemplate) {
}
