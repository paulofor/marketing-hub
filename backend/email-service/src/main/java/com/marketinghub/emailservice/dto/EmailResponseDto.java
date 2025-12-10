package com.marketinghub.emailservice.dto;

import com.marketinghub.emailservice.model.EmailStatus;
import java.time.Instant;

public record EmailResponseDto(
        String requestId,
        EmailStatus status,
        Instant createdAt,
        Instant sentAt,
        String message
) {
}
