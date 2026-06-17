package com.marketinghub.oprmcoletormei.opportunity.pipeline;

import java.util.Map;

/** Contexto genérico recebido por uma etapa do fluxo CNAE de oportunidade. */
public record StageContext<I>(
        String cycleId,
        String stageName,
        I input,
        ArtifactStore artifactStore,
        Map<String, Object> metadata) {}
