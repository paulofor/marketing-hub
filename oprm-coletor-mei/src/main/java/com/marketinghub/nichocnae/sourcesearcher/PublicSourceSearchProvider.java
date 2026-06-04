package com.marketinghub.nichocnae.sourcesearcher;

import java.util.List;

/** Define o contrato de provedores públicos de busca usados pelo coletor OPRM na etapa três. */
public interface PublicSourceSearchProvider {
    /** Executa a frase de pesquisa e devolve resultados públicos normalizados. */
    List<SourceSearchResult> search(String queryText, int maxResults);

    /** Informa o código do provedor salvo no backend junto das fontes candidatas. */
    String providerCode();
}
