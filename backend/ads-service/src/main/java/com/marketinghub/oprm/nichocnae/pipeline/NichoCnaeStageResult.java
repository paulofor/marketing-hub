package com.marketinghub.oprm.nichocnae.pipeline;

import java.util.List;
import java.util.Map;

/** Responsabilidade: padronizar a saída estruturada de uma etapa do pipeline OPRM NichoCNAE. */
public record NichoCnaeStageResult(
        String status,
        Map<String, Object> output,
        List<NichoCnaeStageArtifact> artifacts,
        String errorMessage) {}
