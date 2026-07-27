package com.marketinghub.microservice.exception.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MicroserviceExceptionSummary {
  Instant lastOccurredAt;
  String lastMessage;
  String lastSeverity;
  long totalCount;
}
