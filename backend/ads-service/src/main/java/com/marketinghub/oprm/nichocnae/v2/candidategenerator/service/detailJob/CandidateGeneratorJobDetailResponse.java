package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.detailJob;

import java.math.BigDecimal;
import java.util.List;

/** Detalha o histórico persistido de etapas de um job NichoCNAE v2. */
public record CandidateGeneratorJobDetailResponse(
        String jobId,
        String cnaeCode,
        String status,
        String finalDecision,
        String finalDecisionLabel,
        String finalDecisionReason,
        String outcomeStatus,
        String outcomeMessage,
        String proposedNicheName,
        Long marketNicheId,
        BigDecimal aiCostUsd,
        Boolean canConfirmNiche,
        List<CandidateGeneratorJobStageStep> stages) {}
