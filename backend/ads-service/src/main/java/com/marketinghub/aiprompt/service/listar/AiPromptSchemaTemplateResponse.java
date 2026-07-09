package com.marketinghub.aiprompt.service.listar;

import java.time.Instant;

/** Responsabilidade: representar um template operacional de IA na API administrativa. */
public record AiPromptSchemaTemplateResponse(
        String templateKey,
        String pipelineCode,
        String stageCode,
        String version,
        String openAiModel,
        String schemaName,
        String promptMarkdownContent,
        String schemaJson,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
