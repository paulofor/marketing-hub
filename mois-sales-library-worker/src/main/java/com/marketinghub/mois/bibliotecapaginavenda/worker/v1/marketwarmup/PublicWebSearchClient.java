package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import java.io.IOException;
import java.util.List;

/**
 * Define a porta de coleta de busca web pública usada pela Etapa 3 sem acesso direto ao banco.
 */
public interface PublicWebSearchClient {
    /**
     * Executa uma busca pública para a query informada e devolve resultados rastreáveis.
     */
    List<PublicSearchResult> search(String query, int limit) throws IOException;
}
