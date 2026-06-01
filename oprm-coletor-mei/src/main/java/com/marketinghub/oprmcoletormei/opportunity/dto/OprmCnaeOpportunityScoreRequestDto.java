package com.marketinghub.oprmcoletormei.opportunity.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** DTO usado pelo OPRM para gravar no backend o score de oportunidade calculado. */
public record OprmCnaeOpportunityScoreRequestDto(
        String cnaeDescription,
        BigDecimal opportunityScore,
        BigDecimal marketVolumeScore,
        BigDecimal meiDensityScore,
        BigDecimal digitalFitScore,
        BigDecimal painClarityScore,
        String scoreJustification,
        String algorithmVersion,
        String cycleId,
        Instant scoredAt,
        String scoreStatus) {}
