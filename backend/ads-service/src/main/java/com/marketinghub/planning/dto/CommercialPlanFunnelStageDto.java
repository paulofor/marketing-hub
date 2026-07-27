package com.marketinghub.planning.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: expor uma etapa do funil usada no planejamento comercial. */
public record CommercialPlanFunnelStageDto(
    String code,
    String name,
    Long plannedTotal,
    Long actualTotal,
    BigDecimal conversionFromPreviousStep,
    BigDecimal costPerConversion,
    Long uniqueCount,
    Instant lastEventAt,
    Boolean applicable,
    String evidenceSource) {}
