package com.marketinghub.catalogovivo.v1.service.pending;

/** Responsabilidade: entregar ao executor somente o texto fixado na criação da tarefa. */
public record CatalogPromptResponse(
    String origin,
    long bindingId,
    long versionId,
    int versionNumber,
    String text,
    String sha256,
    String schemaId,
    String schemaSha256,
    String agentKey,
    String activityId,
    int processVersion,
    String executorModule) {}
