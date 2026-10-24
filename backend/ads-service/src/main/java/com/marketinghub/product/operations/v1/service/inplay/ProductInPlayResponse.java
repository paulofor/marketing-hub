package com.marketinghub.product.operations.v1.service.inplay;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

/** Responsabilidade: expor somente a identidade e o trabalho corrente de um produto em PLAY. */
public record ProductInPlayResponse(
    Long productId,
    String productName,
    String commercialName,
    Long chainId,
    Long cycleId,
    String sourceReference,
    Long processDefinitionId,
    Long runId,
    String activityId,
    String activityNumber,
    String activityName,
    String responsible,
    String status,
    String statusLabel,
    String reason,
    String activityUrl,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant lastReconciledAt) {}
