package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.listCnaeJobs;

import java.util.List;

/** Representa as listas de jobs v2 abertos e encerrados para um CNAE. */
public record CandidateGeneratorCnaeJobsResponse(
        String cnaeCode,
        List<CandidateGeneratorCnaeJobSummary> openJobs,
        List<CandidateGeneratorCnaeJobSummary> completedJobs) {}
