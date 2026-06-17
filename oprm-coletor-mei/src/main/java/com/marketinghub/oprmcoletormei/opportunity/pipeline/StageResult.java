package com.marketinghub.oprmcoletormei.opportunity.pipeline;

import java.util.List;
import java.util.Map;

/** Resultado estruturado de uma etapa do fluxo CNAE com saída, artefatos e métricas auditáveis. */
public record StageResult<O>(
        O output,
        List<StageArtifact> artifacts,
        Map<String, Object> metrics) {}
