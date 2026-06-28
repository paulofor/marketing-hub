package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import java.util.List;

/** Define a porta de busca pública usada pela etapa source-searcher do NichoCNAE v3. */
public interface SourceSearchClient {
    /** Executa a busca pública para a consulta informada e devolve resultados rastreáveis. */
    List<SourceSearchResult> search(String query, int limit);
}
