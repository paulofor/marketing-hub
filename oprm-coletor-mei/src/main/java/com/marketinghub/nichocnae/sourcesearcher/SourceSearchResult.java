package com.marketinghub.nichocnae.sourcesearcher;

/** Representa um resultado público normalizado antes de envio ao backend da etapa três. */
public record SourceSearchResult(
        String sourceUrl,
        String sourceTitle,
        String sourceSnippet,
        String sourceDomain,
        Integer searchPosition) {}
