package com.marketinghub.worker.geraanunciov2.pipeline.criativo;

import java.util.Map;

/** Responsabilidade: transportar o contexto recebido do backend para geração de criativos de anúncio. */
public record GeraAnuncioCriativoInput(String stageExecutionId, Long experimentId, String jobId, Map<String, Object> context) {}
