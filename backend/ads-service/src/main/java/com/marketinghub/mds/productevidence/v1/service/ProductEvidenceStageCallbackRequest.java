package com.marketinghub.mds.productevidence.v1.service;

import java.util.List;
import java.util.Map;

/** Contrato recebido do scientific-research-worker ao concluir ou falhar uma etapa científica. */
public record ProductEvidenceStageCallbackRequest(
    String jobId,
    String executionId,
    String status,
    Map<String, Object> output,
    List<Map<String, Object>> artifacts,
    String rootCause,
    String commercialImpact,
    String recommendedAction,
    String nextStageCode,
    String errorType,
    String errorMessage) {}
