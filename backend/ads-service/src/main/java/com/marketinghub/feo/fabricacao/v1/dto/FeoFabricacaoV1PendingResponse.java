package com.marketinghub.feo.fabricacao.v1.dto;

import java.util.Map;

/** Responsabilidade: expor uma execução pendente no contrato consumido pelo worker FEO. */
public record FeoFabricacaoV1PendingResponse(
    String jobId, String executionId, Map<String, Object> input, Map<String, Object> config) {}
