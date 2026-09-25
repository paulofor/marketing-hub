package com.marketinghub.facebookads.resumption.service.view;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Expõe progresso, contrato financeiro e evidência sem serializar JSON dentro de JSON. */
public record ResumeCampaignView(
    Long id,
    Long experimentId,
    String campaignId,
    String adSetId,
    String status,
    BigDecimal totalLimit,
    BigDecimal dailyBudget,
    BigDecimal historicalSpend,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal zeroResultSpendLimit,
    BigDecimal zeroPurchaseSpendLimit,
    Integer purchaseStopCount,
    String reason,
    String destinationUrl,
    Instant requestedAt,
    Instant completedAt,
    String leaseToken,
    String error,
    JsonNode evidence) {}
