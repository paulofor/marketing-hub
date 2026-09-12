package com.marketinghub.businessprocess.automation.v1.service.status;

/** Responsabilidade: identificar uma chamada oficial e seu caminho contextual entre processos. */
public record ProcessRunRelationResponse(
    Long processDefinitionId,
    String processName,
    Integer processVersion,
    String activityId,
    String activityName,
    String navigationUrl) {}
