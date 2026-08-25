package com.marketinghub.salesvideo.referenceanalysis.v1.service.pending;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** Contrato entregue ao executor após claim atômico no backend. */
public record Pending(
    Long executionId,
    Long referenceId,
    String tenantId,
    int attemptNumber,
    String producerExecutionId,
    JsonNode input,
    Instant claimedAt) {}
