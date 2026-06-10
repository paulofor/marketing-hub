package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

/**
 * Representa um resultado bruto retornado por uma busca web pública rastreável.
 */
public record PublicSearchResult(String title, String url, String snippet, String rawPayload) {
}
