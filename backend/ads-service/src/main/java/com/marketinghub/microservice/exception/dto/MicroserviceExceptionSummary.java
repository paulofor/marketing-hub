package com.marketinghub.microservice.exception.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MicroserviceExceptionSummary {
    Instant lastOccurredAt;
    String lastMessage;
    String lastSeverity;
    long totalCount;
}
