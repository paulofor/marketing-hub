package com.marketinghub.catalogovivo.v1.service.catalog;

/** Responsabilidade: expor o vínculo imutável entre atividade, agente, tipo e contrato de saída. */
public record CatalogBinding(
    long id,
    long processId,
    int processVersion,
    long activityDefinitionId,
    String activityId,
    String activityName,
    long agentId,
    String agentKey,
    String agentName,
    long productTypeId,
    String productTypeCode,
    String executorModule,
    String schemaId,
    String schemaSha256,
    Long activeVersionId) {}
