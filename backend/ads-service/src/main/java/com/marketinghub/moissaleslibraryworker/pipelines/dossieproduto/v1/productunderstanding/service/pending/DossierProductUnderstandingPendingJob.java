package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.pending;

import com.marketinghub.moissaleslibraryworker.pipelines.shared.service.PipelinePromptSchemaTemplatePayload;
import java.util.Map;

/** Contrato de trabalho pendente entregue ao executor da etapa entendimento do produto do dossiê MOIS v1. */
public record DossierProductUnderstandingPendingJob(
        String jobId,
        long stageExecutionId,
        long dossierId,
        String workspaceId,
        String stageName,
        Map<String, Object> input,
        PipelinePromptSchemaTemplatePayload promptSchemaTemplate) {
}
