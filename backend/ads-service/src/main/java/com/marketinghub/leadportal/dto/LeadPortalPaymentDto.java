package com.marketinghub.leadportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record LeadPortalPaymentDto(
        Long id,
        Long packageId,
        String submissionId,
        String buyerName,
        String buyerEmail,
        String status,
        String mercadoPagoStatus,
        String mercadoPagoPaymentId,
        String mercadoPagoPreferenceId,
        String paymentType,
        String paymentMethod,
        String rejectionReason,
        BigDecimal amount,
        String currency,
        Instant checkoutExpiresAt,
        Instant paymentApprovedAt,
        Instant deliveredAt,
        Instant createdAt,
        Instant updatedAt,
        List<LeadPortalPaymentHistoryDto> history
) {
}
