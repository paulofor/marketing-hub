package com.marketinghub.nichocnae.sourcefetcher;

/** Define o contrato de coleta curta de metadados e trechos permitidos de uma fonte pública. */
public interface PublicSourceFetcher {
    /** Coleta metadados e trecho curto da fonte candidata sem persistir HTML completo. */
    FetchedSourceSnapshot fetch(SourceFetcherPending pending);
}
