package com.marketinghub.gerasalespage.v1.service;

import java.time.Instant;
import java.util.Map;

/** Contrato entregue ao AI Worker para processar uma etapa do GeraSalesPage v1. */
public record GeraSalesPagePendingResponse(
        Long experimentId,
        String stageCode,
        String jobid,
        Instant executionRequestedAt,
        Map<String, Object> experiment,
        Map<String, Object> promptTemplate,
        Map<String, Object> previousStageOutputs
) {}
