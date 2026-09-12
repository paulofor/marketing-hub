package com.marketinghub.businessprocess.automation.v1.service.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** Responsabilidade: expor uma decisão auditável com dados estruturados sem JSON dentro de JSON. */
public record ProcessRunEventResponse(
    Long id,
    String eventType,
    String status,
    String activityId,
    String message,
    JsonNode details,
    Instant createdAt) {}
