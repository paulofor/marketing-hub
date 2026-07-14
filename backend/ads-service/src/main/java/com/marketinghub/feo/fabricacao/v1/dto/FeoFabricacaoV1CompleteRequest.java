package com.marketinghub.feo.fabricacao.v1.dto;

import java.util.List;
import java.util.Map;

/** Responsabilidade: receber o resultado funcional publicado pelo worker FEO. */
public record FeoFabricacaoV1CompleteRequest(
        String workerId,
        String jobId,
        String status,
        Object output,
        List<Map<String, Object>> artifacts,
        Map<String, Object> metrics,
        String blockReason,
        String nextStageCode) {
}
