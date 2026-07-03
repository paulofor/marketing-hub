package com.marketinghub.moissaleslibraryworker.pipelines.shared.service;

import com.marketinghub.aiprompt.AiPromptSchemaTemplate;

/** Contrato entregue ao worker com prompt e schema ativos resolvidos exclusivamente pelo backend. */
public record PipelinePromptSchemaTemplatePayload(
        String templateKey,
        String pipelineCode,
        String stageCode,
        String version,
        String openAiModel,
        String schemaName,
        String promptMarkdownContent,
        String schemaJson) {

    /** Converte a entidade persistida no banco no payload usado pelos endpoints pending. */
    public static PipelinePromptSchemaTemplatePayload from(AiPromptSchemaTemplate template) {
        return new PipelinePromptSchemaTemplatePayload(
                template.getTemplateKey(),
                template.getPipelineCode(),
                template.getStageCode(),
                template.getVersion(),
                template.getOpenAiModel(),
                template.getSchemaName(),
                template.getPromptMarkdownContent(),
                template.getSchemaJson());
    }
}
