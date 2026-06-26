package com.marketinghub.pipelines.nichocnae.v3.execution;

import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;

/** Definição de etapa v3 com endpoint backend e processor plugável. */
public record NichoCnaeV3StageDefinition(String stageCode, String backendPath, StageProcessor processor) {}
