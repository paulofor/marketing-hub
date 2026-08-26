package com.marketinghub.businessprocess;

/** Responsabilidade: expor a identidade persistida de uma atividade de processo. */
public record BusinessProcessActivityDefinitionResponse(
    Long id,
    String activityId,
    String name,
    String objective,
    String ownerName,
    String executionResourceCode,
    String subprocessCode) {}
