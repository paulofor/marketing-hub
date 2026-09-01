package com.marketinghub.experiment.service.cockpit;

import java.math.BigDecimal;

/** Placar financeiro e de conversão usado pelo cockpit para leitura de vendas. */
public record ExperimentCockpitScoreboardDto(
    BigDecimal spend,
    BigDecimal revenue,
    BigDecimal margin,
    BigDecimal roas,
    Long impressions,
    Long clicks,
    BigDecimal ctr,
    BigDecimal cpc,
    long directContacts,
    int directContactTarget,
    long pageViews,
    long partialVideoViews,
    long completeVideoViews,
    long leads,
    long checkoutAccesses,
    long purchases,
    BigDecimal costPerLead,
    BigDecimal costPerCheckoutAccess,
    BigDecimal costPerPurchase) {}
