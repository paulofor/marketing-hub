package com.marketinghub.nichocnae.pipeline;

import java.util.Map;

/** Representa uma unidade de execução tipada que será processada por uma etapa do pipeline nichocnae. */
public record StageExecution<I>(
        String idJob,
        I input,
        Map<String, Object> config) {}
