package com.marketinghub.nichocnaev2.execution;

import com.marketinghub.nichocnaev2.pipeline.StageProcessor;

/** Define os dados operacionais de uma etapa NichoCNAE v2 consumida pelo agendador genérico. */
public record NichoCnaeV2StageDefinition(String stageCode, String backendPath, StageProcessor processor) {}
