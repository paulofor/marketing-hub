package com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.createStageExecution;

/** Contrato de escrita para criar pendência do controlador de reprocessamento calculado pelo executor externo. */
public record ReprocessControllerCreateRequest(String jobId, Long researchCycleId, Long sourceNicheId, String cnaeCode, Integer attemptNumber, Integer knowledgeVersion, Boolean materializationEnabled, String inputPayload) {}
