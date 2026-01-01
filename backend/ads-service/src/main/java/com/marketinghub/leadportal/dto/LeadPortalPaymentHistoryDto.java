package com.marketinghub.leadportal.dto;

import java.time.Instant;

public record LeadPortalPaymentHistoryDto(
        Instant at,
        String label,
        String status,
        String source,
        String detail
) {
}
