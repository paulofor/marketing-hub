package com.marketinghub.oprm.nichocnae.routineresearchcycle.service.listRecentJobs;

import java.util.List;

/** Representa uma página de jobs recentes do pipeline OPRM NichoCNAE. */
public record OprmNichoCnaeJobsPageResponse(
    List<OprmNichoCnaeJobSummaryResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last) {}
