package com.marketinghub.nichocnae.sourcesearcher;

import java.util.List;

/** Representa o payload que conclui uma query da etapa três no backend. */
public record SourceSearcherCompletionRequest(String searchProvider, List<SourceCandidateRequest> results) {}
