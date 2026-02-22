package com.marketinghub.settings.dto;

import java.time.Instant;

public record TestEmailResponse(
        boolean success,
        String message,
        Instant sentAt
) {
}
