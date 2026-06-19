package com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.pending;

/** Contrato de fila interna entregue ao executor para filtrar fontes inseguras do NichoCNAE v2. */
public record SourceSafetyFilterPendingResponse(
        String stageExecutionId,
        String jobId,
        String cnaeCode,
        Long sourceNicheId,
        Integer attemptNumber,
        Integer technicalRetryNumber,
        Integer knowledgeVersion,
        String inputPayload) {}
