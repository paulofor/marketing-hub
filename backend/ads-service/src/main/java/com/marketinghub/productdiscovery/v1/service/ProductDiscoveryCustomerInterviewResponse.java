package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryInterviewOutcome;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Saída auditável de entrevista sem nome, contato ou identificador externo da pessoa. */
public record ProductDiscoveryCustomerInterviewResponse(
    Long id,
    Long cycleId,
    Long opportunityId,
    String opportunityName,
    String anonymousParticipantCode,
    ProductDiscoveryInterviewOutcome outcome,
    LocalDate occurredOn,
    Instant consentCapturedAt,
    String purchaseSituation,
    String desiredResult,
    String difficulty,
    String alternativeTried,
    BigDecimal amountSpent,
    String currency,
    String remainingDifficulty,
    Instant createdAt) {}
