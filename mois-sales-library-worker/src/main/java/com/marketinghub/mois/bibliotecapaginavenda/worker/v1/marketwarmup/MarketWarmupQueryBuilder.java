package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupClaimedJob;
import java.net.URI;
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
     * Monta queries focadas em descobrir canais, autoridade, funil e provas que explicam o sucesso do produto.
     */
    public List<String> buildQueries(MarketWarmupClaimedJob job) {
        String title = clean(job.title());
        String producer = clean(job.producerName());
        String promise = clean(job.promiseSummary());
        String mechanism = clean(job.mechanismSummary());
        String offer = clean(job.offerSummary());
        String proof = clean(job.proofSummary());
        String domain = extractDomain(job.urlCanonical());
        String titleWithoutSuffix = clean(title.replaceAll("(?i)\\s+-\\s+.*$", ""));
        String titleSuffix = clean(title.replaceAll("(?i)^.*?\\s+-\\s+", ""));
        String productAndProducer = clean(title + " " + producer);
        Set<String> queries = new LinkedHashSet<>();
        if (!producer.isBlank() && !clean(title + " " + promise + " " + mechanism).isBlank()) {
            addIfUseful(queries, exact(producer) + " " + exact(title) + " Instagram YouTube TikTok");
            addIfUseful(queries, exact(producer) + " " + promise + " " + mechanism + " canal perfil especialista");
        }
        if (!productAndProducer.isBlank()) {
            addIfUseful(queries, exact(title) + " " + producer + " Instagram YouTube TikTok influenciador fundador");
            addIfUseful(queries, productAndProducer + " depoimento review funciona vale a pena reclamação");
            addIfUseful(queries, productAndProducer + " aula gratuita live whatsapp comunidade lançamento");
            addIfUseful(queries, productAndProducer + " afiliado hotmart produtor oferta bônus");
        }
        if (!domain.isBlank()) {
            addIfUseful(queries, exact(domain) + " Instagram YouTube TikTok depoimentos");
            addIfUseful(queries, exact(domain) + " " + producer + " review funciona");
        }
        if (!titleWithoutSuffix.isBlank() && !titleSuffix.equals(titleWithoutSuffix)) {
            addIfUseful(queries, exact(titleWithoutSuffix) + " " + exact(titleSuffix) + " Instagram YouTube TikTok");
            addIfUseful(queries, exact(titleWithoutSuffix) + " " + titleSuffix + " depoimentos review funciona");
        }
        if (!producer.isBlank()) {
            addIfUseful(queries, exact(producer) + " autoridade seguidores alunas método " + firstUseful(title, promise));
        }
        if (!clean(promise + " " + mechanism).isBlank()) {
            addIfUseful(queries, promise + " " + mechanism + " canal influencer especialista");
        }
        if (!clean(offer + " " + proof).isBlank()) {
            addIfUseful(queries, offer + " " + proof + " prova social depoimentos resultados");
        }
        return queries.stream().limit(10).toList();
    }

    /**
     * Extrai o domínio da página final para achar presença pública quando título e produtor são fracos.
     */
    private String extractDomain(String url) {
        try {
            String host = URI.create(clean(url)).getHost();
            if (host == null || host.isBlank()) {
                return "";
            }
            return host.replaceFirst("^www\\.", "");
        } catch (RuntimeException ex) {
            return "";
        }
    }

    /**
     * Escolhe o primeiro texto comercial útil para ancorar a busca do produtor.
     */
    private String firstUseful(String first, String second) {
        String primary = clean(first);
        return primary.isBlank() ? clean(second) : primary;
    }

    /**
     * Adiciona query apenas quando há conteúdo comercial suficiente para evitar buscas genéricas demais.
     */
    private void addIfUseful(Set<String> queries, String query) {
        String normalized = clean(query);
        if (normalized.length() >= 18 && hasSpecificSignal(normalized)) {
            queries.add(normalized);
        }
    }

    /**
     * Exige termos de produto, produtor ou ativos de venda para reduzir resultados genéricos por palavras soltas.
     */
    private boolean hasSpecificSignal(String query) {
        return query.contains("\"")
                || query.toLowerCase().contains("instagram")
                || query.toLowerCase().contains("youtube")
                || query.toLowerCase().contains("hotmart")
                || query.toLowerCase().contains("whatsapp")
                || query.toLowerCase().contains("depoimento")
                || query.toLowerCase().contains("alunas")
                || query.toLowerCase().contains("seguidores");
    }

    /**
     * Protege nomes de produtos para que a busca trate o título como uma expressão e não como termos genéricos isolados.
     */
    private String exact(String value) {
        String normalized = clean(value);
        return normalized.isBlank() ? "" : "\"" + normalized.replace("\"", "") + "\"";
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
