package com.marketinghub.nichocnaev3.execution;

import com.marketinghub.nichocnaev3.pipeline.StageProcessor;

/** Definição de etapa v3 com endpoint backend e processor plugável. */
public record NichoCnaeV3StageDefinition(String stageCode, String backendPath, StageProcessor processor) {}
