package com.marketinghub.financialplan.v1.service.prepareplan;

import java.math.BigDecimal;

/** Responsabilidade: apresentar sugestões e referências resolvidas pelo backend para preparação. */
public record PlanPreparation(
    int expectedRevision,
    Long sourceRevisionId,
    Long commercialPlanId,
    Integer commercialPlanVersion,
    String productVersion,
    int supportDays,
    boolean personalizedAi,
    String suggestion,
    BigDecimal priceBrl,
    boolean canPrepare,
    String blocker) {}
