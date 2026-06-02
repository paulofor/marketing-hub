package com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution;

import java.util.List;

/** Representa o payload de conclusão da etapa três com provedor e resultados encontrados. */
public record CompleteSourceSearcherRequest(String searchProvider, List<SourceCandidateRequest> results) {}
