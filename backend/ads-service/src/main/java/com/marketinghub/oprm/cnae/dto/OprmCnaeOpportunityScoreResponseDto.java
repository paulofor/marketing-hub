package com.marketinghub.oprm.cnae.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO de resposta com score de oportunidade de CNAE persistido pelo backend.
 */
public record OprmCnaeOpportunityScoreResponseDto(
        String cnaeCode,
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
        String scoreStatus,
        Instant enrichedAt) {}
