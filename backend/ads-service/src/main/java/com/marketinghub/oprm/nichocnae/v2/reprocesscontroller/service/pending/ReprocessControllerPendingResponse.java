package com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.pending;

/** Contrato de leitura de pendência da etapa reprocess-controller para o executor OPRM. */
public record ReprocessControllerPendingResponse(String stageExecutionId, String jobId, String cnaeCode, Long sourceNicheId, Integer attemptNumber, Integer technicalRetryNumber, Integer knowledgeVersion, String inputPayload) {}
