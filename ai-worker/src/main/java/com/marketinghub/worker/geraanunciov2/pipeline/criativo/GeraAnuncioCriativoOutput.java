package com.marketinghub.worker.geraanunciov2.pipeline.criativo;

import java.util.List;
import java.util.Map;

/** Responsabilidade: transportar os anúncios estruturados gerados pela etapa Criativo. */
public record GeraAnuncioCriativoOutput(List<Map<String, Object>> ads, Map<String, Object> audit) {}
