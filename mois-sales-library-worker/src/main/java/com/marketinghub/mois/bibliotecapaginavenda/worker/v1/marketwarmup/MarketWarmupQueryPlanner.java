package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupClaimedJob;
import java.util.List;

/**
 * Planeja buscas públicas qualificadas para descobrir autoridade, canais e prova social do dossiê.
 */
public interface MarketWarmupQueryPlanner {
    /**
     * Recebe as buscas heurísticas e devolve buscas enriquecidas, mantendo as originais como fallback seguro.
     */
    List<String> planQueries(MarketWarmupClaimedJob job, List<String> baseQueries);
}
