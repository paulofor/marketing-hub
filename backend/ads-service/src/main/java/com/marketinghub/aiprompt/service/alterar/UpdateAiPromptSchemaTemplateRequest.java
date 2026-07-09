package com.marketinghub.aiprompt.service.alterar;

/** Responsabilidade: receber alterações administrativas de prompt/schema operacional de IA. */
public record UpdateAiPromptSchemaTemplateRequest(
        String version,
        String openAiModel,
        String schemaName,
        String promptMarkdownContent,
        String schemaJson,
        Boolean active) {}
