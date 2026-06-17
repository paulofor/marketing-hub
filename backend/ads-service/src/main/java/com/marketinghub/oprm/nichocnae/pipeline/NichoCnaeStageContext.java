package com.marketinghub.oprm.nichocnae.pipeline;

import java.util.Map;

/** Responsabilidade: transportar o contexto genérico necessário para executar uma etapa OPRM NichoCNAE. */
public record NichoCnaeStageContext(
        String jobId,
        Long cycleId,
        String cnaeCode,
        OprmNichoCnaePipelineSection section,
        Map<String, Object> inputs) {}
