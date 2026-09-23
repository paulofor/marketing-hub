package com.marketinghub.facebookads.resumption.service.request;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Registra orçamento, janela e condições de parada consentidos para uma retomada de campanha. */
public record ResumeCampaignRequest(
    BigDecimal totalLimit,
    BigDecimal dailyBudget,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal zeroResultSpendLimit,
    BigDecimal zeroPurchaseSpendLimit,
    Integer purchaseStopCount,
    String reason,
    boolean authorizeSpending,
    boolean useTotalLimitForZeroResults) {}
