package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupClaimedJob;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Gera queries de pesquisa pública a partir da análise comercial já concluída da página.
 */
@Component
public class MarketWarmupQueryBuilder {
    /**
     * Monta queries focadas em produto, dor, promessa, prova e concorrentes para a Etapa 3.
     */
    public List<String> buildQueries(MarketWarmupClaimedJob job) {
        String title = clean(job.title());
        String promise = clean(job.promiseSummary());
        String mechanism = clean(job.mechanismSummary());
        String offer = clean(job.offerSummary());
        String proof = clean(job.proofSummary());
        Set<String> queries = new LinkedHashSet<>();
        addIfUseful(queries, title + " review depoimento reclamação");
        addIfUseful(queries, title + " YouTube Instagram TikTok");
        addIfUseful(queries, promise + " como resolver");
        addIfUseful(queries, mechanism + " método resultados");
        addIfUseful(queries, offer + " concorrentes afiliados");
        addIfUseful(queries, proof + " antes depois depoimentos");
        return queries.stream().limit(6).toList();
    }

    /**
     * Adiciona query apenas quando há conteúdo comercial suficiente para evitar buscas genéricas demais.
     */
    private void addIfUseful(Set<String> queries, String query) {
        String normalized = clean(query);
        if (normalized.length() >= 12) {
            queries.add(normalized);
        }
    }

    /**
     * Normaliza espaços e limita trechos longos para preservar foco da pesquisa pública.
     */
    private String clean(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 140 ? normalized.substring(0, 140).trim() : normalized;
    }
}
