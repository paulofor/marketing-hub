package com.marketinghub.pipelines.dossie.v1;

import java.util.Map;

/** Carrega o contexto funcional persistido pelo backend para execução de uma etapa do dossiê MOIS v1. */
public record StageContext(
        long stageExecutionId,
        long dossierId,
        String workspaceId,
        String stageName,
        Map<String, Object> input,
        PromptSchemaTemplate promptSchemaTemplate) {

    /** Cria contexto sem template para etapas locais que não usam modelo de IA. */
    public StageContext(long stageExecutionId, long dossierId, String workspaceId, String stageName, Map<String, Object> input) {
        this(stageExecutionId, dossierId, workspaceId, stageName, input, null);
    }

    /** Representa o prompt/schema versionado recebido do backend pelo endpoint pending. */
    public record PromptSchemaTemplate(
            String templateKey,
            String pipelineCode,
            String stageCode,
            String version,
            String openAiModel,
            String schemaName,
            String promptMarkdownContent,
            String schemaJson) {
    }
}
