package com.marketinghub.productaiworker.core;

import java.util.Map;

/** Responsabilidade: transportar uma execução pendente sem acoplar o núcleo a uma etapa concreta. */
public record StageContext(
        String idJob,
        Long purchaseId,
        Long packageId,
        Long experimentId,
        String pipelineCode,
        String stageCode,
        Map<String, Object> buyer,
        Map<String, Object> personalizationInput,
        Map<String, Object> experiment,
        Map<String, Object> promptSchemaTemplate) {}
