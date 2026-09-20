package com.marketinghub.facebookads.resumption.service.request;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Registra consentimento explícito para teto acumulado e exceção individual de coleta. */
public record ResumeCampaignRequest(
    BigDecimal totalLimit,
    LocalDate endDate,
    String reason,
    boolean authorizeSpending,
    boolean useTotalLimitForZeroResults) {}
