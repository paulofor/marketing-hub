package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

/** Representa um resultado bruto rastreável retornado por provedor de busca pública. */
public record SourceSearchResult(String title, String url, String snippet, String provider, String rawPayload) {}
