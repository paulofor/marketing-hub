package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSearchAttemptCompleteItem;
import java.util.List;

/**
 * Representa falha de dossiê sem fontes qualificadas preservando as tentativas feitas para auditoria.
 */
public class MarketWarmupNoQualifiedSourcesException extends IllegalStateException {
    private final List<MarketWarmupSearchAttemptCompleteItem> searchAttempts;

    /**
     * Cria a exceção com mensagem operacional e tentativas de busca já estruturadas.
     */
    public MarketWarmupNoQualifiedSourcesException(String message, List<MarketWarmupSearchAttemptCompleteItem> searchAttempts) {
        super(message);
        this.searchAttempts = searchAttempts == null ? List.of() : List.copyOf(searchAttempts);
    }

    /**
     * Retorna as tentativas de busca que não produziram fonte rastreável.
     */
    public List<MarketWarmupSearchAttemptCompleteItem> searchAttempts() {
        return searchAttempts;
    }
}
