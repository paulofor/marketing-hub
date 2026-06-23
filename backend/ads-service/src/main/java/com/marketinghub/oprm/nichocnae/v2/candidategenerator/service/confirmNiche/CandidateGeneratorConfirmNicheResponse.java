package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.confirmNiche;

import java.math.BigDecimal;
import java.time.Instant;

/** Contrato devolvido após transformar o job NichoCNAE v2 aprovado em MarketNiche. */
public record CandidateGeneratorConfirmNicheResponse(
        String jobId,
        String cnaeCode,
        Long marketNicheId,
        String nicheName,
        BigDecimal aiCostUsd,
        String status,
        String message,
        Instant updatedAt) {}
