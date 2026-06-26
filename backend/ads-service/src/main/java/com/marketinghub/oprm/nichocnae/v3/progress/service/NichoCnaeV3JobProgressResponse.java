package com.marketinghub.oprm.nichocnae.v3.progress.service;

import java.util.List;

/** Representa o progresso do job mais recente de um CNAE no pipeline NichoCNAE v3. */
public record NichoCnaeV3JobProgressResponse(String jobId, String cnaeCode, List<NichoCnaeV3StageProgressResponse> stages, NichoCnaeV3FinalizationReviewResponse finalizationReview) {}
