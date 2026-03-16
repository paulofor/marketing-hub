package com.marketinghub.payments.dto;

import com.marketinghub.payments.model.FlowSubmissionImagePackageStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LeadPortalPackageSummary(
        long packageId,
        UUID submissionId,
        String submissionName,
        String submissionEmail,
        FlowSubmissionImagePackageStatus status,
        String prompt,
        String model,
        BigDecimal totalPrice,
        String currency,
        Instant createdAt,
        Long flowId,
        String flowSlug,
        Long experimentId,
        BigDecimal experimentUnitPriceBrl
) {
}
