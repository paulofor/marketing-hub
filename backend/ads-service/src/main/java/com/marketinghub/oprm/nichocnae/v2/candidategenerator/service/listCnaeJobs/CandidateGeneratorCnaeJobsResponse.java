package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.listCnaeJobs;

import java.math.BigDecimal;
import java.util.List;

/** Representa as listas de jobs v2 abertos e encerrados para um CNAE. */
public record CandidateGeneratorCnaeJobsResponse(
        String cnaeCode,
        BigDecimal cnaeAiCostUsd,
        Boolean cnaeUsedAi,
        List<CandidateGeneratorCnaeJobSummary> openJobs,
        List<CandidateGeneratorCnaeJobSummary> completedJobs) {}
